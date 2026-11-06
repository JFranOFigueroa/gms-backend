package com.gymback.checkin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymback.modules.checkin.services.CheckinCrypto;
import com.gymback.security.DeviceAuthorizationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.time.*;
import java.util.concurrent.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP, SecurityFilterChain, Flyway and PostgreSQL. Never targets the normal database. */
@EnabledIfEnvironmentVariable(named="GMS_TEST_DB_URL", matches="jdbc:postgresql://127\\.0\\.0\\.1:55432/gms_checkin_audit")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
 "spring.config.import=", "app.user-api-enabled=true", "app.jwt.secret=audit-only-jwt-secret-at-least-32-bytes-001",
 "app.checkin.member-qr-secret=audit-only-member-secret-at-least-32-bytes-002",
 "app.checkin.kiosk-csrf-secret=audit-only-csrf-secret-at-least-32-bytes-003",
 "app.checkin.cookie-secure=false", "app.frontend-origin=http://localhost:5173",
 "spring.jpa.properties.hibernate.default_schema=checkin_audit", "spring.flyway.default-schema=checkin_audit",
 "spring.flyway.schemas=checkin_audit", "spring.flyway.placeholders.schema_db=checkin_audit",
 "spring.datasource.hikari.connection-init-sql=SET search_path TO checkin_audit, public"})
class KioskPostgresIntegrationTest {
 @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
  r.add("spring.datasource.url",()->System.getenv("GMS_TEST_DB_URL"));
  r.add("spring.datasource.username",()->"gms_audit");
  r.add("spring.datasource.password",()->System.getenv("GMS_TEST_DB_PASSWORD"));
  r.add("app.frontend-origin",KioskPostgresIntegrationTest::origin);
 }
 static String origin(){return System.getenv().getOrDefault("GMS_TEST_FRONTEND_ORIGIN","http://localhost:5173");}
 @LocalServerPort int port;
 @Autowired JdbcTemplate db;
 @Autowired ObjectMapper json;
 @Autowired PasswordEncoder passwords;
 @Autowired CheckinCrypto crypto;
 @Autowired DeviceAuthorizationService authorization;
 @Autowired JwtEncoder jwtEncoder;
 UUID gym; String slug, ownerToken,ownerRefresh;
 CookieManager cookies;
 HttpClient client;
 @BeforeEach void setup() throws Exception {
  gym=UUID.randomUUID(); slug="audit-"+gym;
  db.update("insert into gym(id,name,slug) values (?,?,?)",gym,"Audit Gym",slug);
  db.update("insert into gym_modules(gym_id,module_key,enabled) values (?,'checkin',true) on conflict (gym_id,module_key) do update set enabled=true",gym);
  UUID owner=UUID.randomUUID();String email="audit-"+owner+"@example.test";
  db.update("insert into users(id,full_name,email,password_hash,status) values (?, ?, ?, ?, 'active')",owner,"Audit Owner",email,passwords.encode("audit-password"));
  db.update("insert into gym_staff_roles(gym_id,user_id,role) values (?,?,'owner')",gym,owner);
  cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);client=HttpClient.newBuilder().cookieHandler(cookies).build();
  var login=call("POST","/auth/login",Map.of("email",email,"password","audit-password"),null,null,null);
  assertStatus(200,login);ownerToken=body(login).path("token").asText();ownerRefresh=body(login).path("refreshToken").asText();
 }
 HttpResponse<String> call(String method,String path,Object payload,String jwt,String csrf,String key) throws Exception {
  var b=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1"+path)).header("Origin",origin()).timeout(Duration.ofSeconds(15));
  if(jwt!=null)b.header("Authorization","Bearer "+jwt);
  if(csrf!=null)b.header("X-CSRF-Token",csrf);
  if(key!=null)b.header("Idempotency-Key",key);
  if(payload!=null)b.header("Content-Type","application/json");
  b.method(method,payload==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
  return client.send(b.build(),HttpResponse.BodyHandlers.ofString());
 }
 JsonNode body(HttpResponse<String> r) throws Exception {return json.readTree(r.body());}
 void assertStatus(int expected,HttpResponse<String> r){assertEquals(expected,r.statusCode(),"HTTP status for "+r.uri().getPath());}
 void sameDecision(JsonNode a,JsonNode b){
  for(String field:List.of("allowed","reasonCode","message","visitDate"))assertEquals(a.path(field),b.path(field));
  // PostgreSQL stores microseconds and the JDBC session uses UTC; compare instants.
  assertTrue(Math.abs(Duration.between(OffsetDateTime.parse(a.path("occurredAt").asText()),OffsetDateTime.parse(b.path("occurredAt").asText())).toNanos())<1000);
 }
 JsonNode create() throws Exception {
  var r=call("POST","/gyms/"+gym+"/checkin-devices",Map.of("deviceName","Audit reception"),ownerToken,null,null);
  assertStatus(201,r);return body(r);
 }
 String activate(JsonNode device) throws Exception {
  var r=call("POST","/kiosk/activate",Map.of("slug",slug,"activationCode",device.path("activationCode").asText()),null,null,null);
  assertStatus(204,r);assertTrue(r.headers().firstValue("set-cookie").isPresent());
  var c=cookies.getCookieStore().getCookies().stream().filter(v->v.getName().equals("GMS_KIOSK")).findFirst().orElseThrow();
  assertTrue(c.isHttpOnly());assertEquals("/api/v1/kiosk",c.getPath());
  assertEquals(1,db.queryForObject("select count(*) from checkin_device_sessions where session_token_hash=? and revoked_at is null and expires_at>now()",Integer.class,crypto.hash(c.getValue())));
  var session=call("GET","/kiosk/session",null,null,null,null);
  assertStatus(200,session);assertEquals(slug,body(session).path("slug").asText());
  assertEquals(gym,authorization.authenticate(c.getValue()).gymId());
  return body(session).path("csrfToken").asText();
 }
 @Test void activationCookieSessionQrRoundTrip() throws Exception {
  var config=call("GET","/public/gyms/"+slug+"/checkin-config",null,null,null,null);
  assertStatus(200,config);assertTrue(body(config).path("checkinAvailable").asBoolean());
  String csrf=activate(create());
  assertStatus(200,call("GET","/kiosk/session",null,null,null,null));
  var qr=call("POST","/kiosk/qr/issue",null,null,csrf,null);assertStatus(200,qr);
  assertFalse(body(qr).path("qrValue").asText().isBlank());
  assertEquals(1,db.queryForObject("select count(*) from checkin_device_sessions where gym_id=? and last_seen_at is not null",Integer.class,gym));
 }
 record Member(UUID id,UUID user,String token){}
 String jwt(UUID user){
  return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
   JwtClaimsSet.builder().subject(user.toString()).issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).issuer("gym-back").build())).getTokenValue();
 }
 Member member(String state){
  UUID id=UUID.randomUUID(),user=UUID.randomUUID(),plan=UUID.randomUUID();
  db.update("insert into users(id,full_name,email,password_hash,status) values (?,'Audit Member',?,?,'active')",user,"audit-"+user+"@example.test",passwords.encode("audit-password"));
  db.update("insert into gym_members(id,gym_id,user_id,member_code,is_active) values (?,?,?,?,?)",id,gym,user,"AUD-"+id,!state.equals("member_inactive"));
  if(state.equals("user_inactive"))db.update("update users set status='disabled' where id=?",user);
  if(!state.equals("no_active_subscription")){
   db.update("insert into membership_plans(id,gym_id,name,billing_period,price) values (?,?,?,'monthly',100)",plan,gym,"Audit plan "+id);
   db.update("insert into member_subscriptions(gym_id,member_id,plan_id,started_on,next_due_date,last_payment_date) values (?,?,?,current_date-30,?,?)",gym,id,plan,
    LocalDate.now().plusDays(state.equals("expired")?-1:30),state.equals("no_payment")?null:LocalDate.now().minusDays(10));
  }
  return new Member(id,user,jwt(user));
 }
 String memberPath(){return "/gyms/"+gym+"/checkins";}
 JsonNode issue(Member m,String kind) throws Exception {
  var r=call("POST",memberPath()+"/me/"+kind,null,m.token(),null,null);assertStatus(200,r);return body(r);
 }
 JsonNode decision(String route,String field,String value,String csrf,String key) throws Exception {
  var r=call("POST","/kiosk/checkins/"+route,Map.of(field,value),null,csrf,key);assertStatus(200,r);
  var d=body(r);assertFalse(d.has("memberId"));assertFalse(d.has("fullName"));assertFalse(d.has("email"));return d;
 }
 @Test void gymQrAccessAndDuplicateAndHistoryStats() throws Exception {
  String csrf=activate(create());Member m=member("active");
  var qr=call("POST","/kiosk/qr/issue",null,null,csrf,null);assertStatus(200,qr);
  var payload=Map.of("kioskToken",body(qr).path("qrValue").asText());String key=UUID.randomUUID().toString();
  var granted=call("POST",memberPath()+"/scan-gym-qr",payload,m.token(),null,key);assertStatus(200,granted);assertTrue(body(granted).path("allowed").asBoolean());
  sameDecision(body(granted),body(call("POST",memberPath()+"/scan-gym-qr",payload,m.token(),null,key)));
  var duplicate=call("POST",memberPath()+"/scan-gym-qr",payload,m.token(),null,UUID.randomUUID().toString());assertStatus(200,duplicate);assertEquals("duplicate",body(duplicate).path("reasonCode").asText());
  var mine=call("GET",memberPath()+"/me",null,m.token(),null,null);assertStatus(200,mine);assertEquals(2,body(mine).path("items").size());
  var audit=call("GET",memberPath(),null,ownerToken,null,null);assertStatus(200,audit);assertEquals("Audit Member",body(audit).get(0).path("member_name").asText());
  var stats=call("GET",memberPath()+"/stats",null,ownerToken,null,null);assertStatus(200,stats);assertEquals(1,body(stats).path("granted").asInt());assertEquals(1,body(stats).path("denied").asInt());
 }
 @Test void memberQrAccessExpirationTamperingAndWrongGym() throws Exception {
  String csrf=activate(create());Member m=member("active");var issued=issue(m,"qr");
  assertEquals(30,Duration.between(OffsetDateTime.parse(issued.path("serverTime").asText()),OffsetDateTime.parse(issued.path("expiresAt").asText())).toSeconds());
  assertTrue(decision("scan-member-qr","memberQr",issued.path("qrValue").asText(),csrf,UUID.randomUUID().toString()).path("allowed").asBoolean());
  String old=crypto.memberQr(gym,m.id(),OffsetDateTime.now().minusSeconds(31));
  assertEquals("token_expired",decision("scan-member-qr","memberQr",old,csrf,UUID.randomUUID().toString()).path("reasonCode").asText());
  assertEquals("invalid_token",decision("scan-member-qr","memberQr",issued.path("qrValue").asText()+"x",csrf,UUID.randomUUID().toString()).path("reasonCode").asText());
  assertEquals("wrong_gym",decision("scan-member-qr","memberQr",crypto.memberQr(UUID.randomUUID(),m.id(),OffsetDateTime.now()),csrf,UUID.randomUUID().toString()).path("reasonCode").asText());
 }
 @Test void manualCodeLifecycleIdempotencyAndRateLimits() throws Exception {
  String csrf=activate(create());Member m=member("active");JsonNode first=issue(m,"code"),second=issue(m,"code");
  assertEquals(120,Duration.between(OffsetDateTime.parse(second.path("serverTime").asText()),OffsetDateTime.parse(second.path("expiresAt").asText())).toSeconds());
  assertEquals("invalid_code",decision("validate-code","accessCode",first.path("accessCode").asText(),csrf,UUID.randomUUID().toString()).path("reasonCode").asText());
  String key=UUID.randomUUID().toString(),value=second.path("accessCode").asText();
  JsonNode granted=decision("validate-code","accessCode",value,csrf,key);assertTrue(granted.path("allowed").asBoolean());
  sameDecision(granted,decision("validate-code","accessCode",value,csrf,key));
  assertEquals("invalid_code",decision("validate-code","accessCode",value,csrf,UUID.randomUUID().toString()).path("reasonCode").asText());
  assertStatus(409,call("POST","/kiosk/checkins/validate-code",Map.of("accessCode",first.path("accessCode").asText()),null,csrf,key));
  issue(m,"code");assertStatus(429,call("POST",memberPath()+"/me/code",null,m.token(),null,null));
  for(int i=0;i<7;i++)decision("validate-code","accessCode","ABCD-EFGH-JKLM",csrf,UUID.randomUUID().toString());
  assertStatus(429,call("POST","/kiosk/checkins/validate-code",Map.of("accessCode","ABCD-EFGH-JKLM"),null,csrf,UUID.randomUUID().toString()));
 }
 @Test void expiredAndForeignManualCodesCannotGrantAccess() throws Exception {
  String csrf=activate(create());Member m=member("active");String expired=issue(m,"code").path("accessCode").asText();
  db.update("update checkin_access_codes set issued_at=now()-interval '121 seconds',expires_at=now()-interval '1 second' where gym_id=?",gym);
  assertEquals("invalid_code",decision("validate-code","accessCode",expired,csrf,UUID.randomUUID().toString()).path("reasonCode").asText());
  String valid=issue(m,"code").path("accessCode").asText();UUID original=gym;
  gym=UUID.randomUUID();slug="audit-"+gym;db.update("insert into gym(id,name,slug) values (?,'Other Audit Gym',?)",gym,slug);
  db.update("update gym_modules set enabled=true where gym_id=? and module_key='checkin'",gym);
  db.update("insert into gym_staff_roles(gym_id,user_id,role) select ?,user_id,'owner' from gym_staff_roles where gym_id=?",gym,original);
  csrf=activate(create());assertEquals("invalid_code",decision("validate-code","accessCode",valid,csrf,UUID.randomUUID().toString()).path("reasonCode").asText());
 }
 @ParameterizedTest @CsvSource({"active,granted", "no_payment,no_payment", "expired,expired", "member_inactive,member_inactive", "no_active_subscription,no_active_subscription"})
 void businessPolicyIsAppliedByBackend(String state,String reason) throws Exception {
  String csrf=activate(create());Member m=member(state.equals("member_inactive")?"active":state);String code=issue(m,"code").path("accessCode").asText();
  if(state.equals("member_inactive"))db.update("update gym_members set is_active=false where id=?",m.id());
  var result=decision("validate-code","accessCode",code,csrf,UUID.randomUUID().toString());
  assertEquals(reason,result.path("reasonCode").asText());assertEquals(state.equals("active"),result.path("allowed").asBoolean());
 }
 @Test void deviceReissueDisableAndSessionExpiryRevokeAccess() throws Exception {
  JsonNode device=create();String csrf=activate(device),path="/gyms/"+gym+"/checkin-devices/"+device.path("deviceId").asText();
  var newCode=call("POST",path+"/reissue-activation",null,ownerToken,null,null);assertStatus(200,newCode);
  assertStatus(401,call("GET","/kiosk/session",null,null,null,null));
  csrf=activate(body(newCode));
  assertStatus(200,call("PATCH",path,Map.of("active",false),ownerToken,null,null));
  assertStatus(401,call("POST","/kiosk/qr/issue",null,null,csrf,null));
  assertStatus(200,call("PATCH",path,Map.of("active",true),ownerToken,null,null));
  csrf=activate(body(call("POST",path+"/reissue-activation",null,ownerToken,null,null)));
  String expiredSecret=crypto.newToken();
  db.update("insert into checkin_device_sessions(gym_id,device_id,session_token_hash,created_at,expires_at) values (?,?,?,now()-interval '13 hours',now()-interval '1 second')",gym,UUID.fromString(device.path("deviceId").asText()),crypto.hash(expiredSecret));
  cookies.getCookieStore().removeAll();HttpCookie expiredCookie=new HttpCookie("GMS_KIOSK",expiredSecret);expiredCookie.setVersion(0);expiredCookie.setPath("/api/v1/kiosk");cookies.getCookieStore().add(URI.create("http://localhost:"+port),expiredCookie);
  assertStatus(401,call("GET","/kiosk/session",null,null,null,null));
 }
 @Test void csrfOriginJwtPermissionsAndLogoutAreEnforced() throws Exception {
  assertStatus(401,call("GET","/kiosk/session",null,null,null,null));
  var device=create();String csrf=activate(device);
  assertStatus(403,call("POST","/kiosk/qr/issue",null,null,null,null));
  assertStatus(403,call("POST","/kiosk/qr/issue",null,null,"wrong",null));
  var evil=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/kiosk/qr/issue")).header("Origin","https://evil.example").header("X-CSRF-Token",csrf).POST(HttpRequest.BodyPublishers.noBody()).build();
  assertStatus(403,client.send(evil,HttpResponse.BodyHandlers.ofString()));
  var preflight=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/v1/kiosk/qr/issue")).header("Origin",origin()).header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","X-CSRF-Token,Idempotency-Key").method("OPTIONS",HttpRequest.BodyPublishers.noBody()).build();
  var cors=client.send(preflight,HttpResponse.BodyHandlers.ofString());assertStatus(200,cors);assertEquals("true",cors.headers().firstValue("Access-Control-Allow-Credentials").orElse(""));
  Member m=member("active");assertStatus(403,call("GET",memberPath()+"/stats",null,m.token(),null,null));
  assertStatus(401,call("GET",memberPath(),null,null,null,null));
  assertStatus(401,call("GET","/auth/me",null,"invalid",null,null));
  assertStatus(403,call("POST","/kiosk/activate",Map.of("slug",slug,"activationCode",device.path("activationCode").asText()),null,null,null));
  assertStatus(204,call("POST","/kiosk/logout",null,null,csrf,null));
  assertStatus(401,call("GET","/kiosk/session",null,null,null,null));
 }
 @Test void concurrentCodeRedemptionsAndSameKeyRetriesAreSerialized() throws Exception {
  String csrf=activate(create());Member m=member("active");String value=issue(m,"code").path("accessCode").asText(),key=UUID.randomUUID().toString();
  ExecutorService workers=Executors.newFixedThreadPool(2);
  try {
   Callable<JsonNode> task=()->decision("validate-code","accessCode",value,csrf,key);
   var a=workers.submit(task);var b=workers.submit(task);
   assertTrue(a.get(15,TimeUnit.SECONDS).path("allowed").asBoolean());sameDecision(a.get(),b.get());
   assertEquals(1,db.queryForObject("select count(*) from checkin_events where gym_id=?",Integer.class,gym));
  }finally{workers.shutdownNow();}
 }
 @Test void legacyAdministrativeCheckinAndRoleIsolation() throws Exception {
  Member m=member("active");
  var r=call("POST",memberPath(),Map.of("memberId",m.id()),ownerToken,null,null);assertStatus(201,r);assertTrue(body(r).path("allowed").asBoolean());
  assertStatus(403,call("POST",memberPath(),Map.of("memberId",m.id()),m.token(),null,null));
  assertStatus(403,call("GET","/gyms/"+UUID.randomUUID()+"/checkins",null,ownerToken,null,null));
 }
 @Test void gymQrExpiryAndWrongGymAreDenied() throws Exception {
  String csrf=activate(create());Member m=member("active");
  var issued=call("POST","/kiosk/qr/issue",null,null,csrf,null);assertStatus(200,issued);
  String raw=body(issued).path("qrValue").asText();
  db.update("update checkin_qr_tokens set issued_at=now()-interval '11 seconds',expires_at=now()-interval '1 second' where gym_id=?",gym);
  var expired=call("POST",memberPath()+"/scan-gym-qr",Map.of("kioskToken",raw),m.token(),null,UUID.randomUUID().toString());assertStatus(200,expired);assertEquals("token_expired",body(expired).path("reasonCode").asText());
  UUID other=UUID.randomUUID();db.update("insert into gym(id,name,slug) values (?,'Foreign Audit Gym',?)",other,"audit-"+other);
  db.update("update gym_modules set enabled=true where gym_id=?",other);
  db.update("insert into gym_members(gym_id,user_id,member_code) values (?,?,'AUD-OTHER')",other,m.user());
  var wrong=call("POST","/gyms/"+other+"/checkins/scan-gym-qr",Map.of("kioskToken",raw),m.token(),null,UUID.randomUUID().toString());assertStatus(200,wrong);assertEquals("wrong_gym",body(wrong).path("reasonCode").asText());
 }
 @Test void suspendedGymAndDisabledModuleAndInvalidCookieAreRejected() throws Exception {
  activate(create());db.update("update gym_modules set enabled=false where gym_id=? and module_key='checkin'",gym);
  assertStatus(401,call("GET","/kiosk/session",null,null,null,null));
  db.update("update gym_modules set enabled=true where gym_id=? and module_key='checkin'",gym);
  db.update("update gym set status='suspended' where id=?",gym);assertStatus(401,call("GET","/kiosk/session",null,null,null,null));
  db.update("update gym set status='active' where id=?",gym);
  cookies.getCookieStore().removeAll();HttpCookie invalid=new HttpCookie("GMS_KIOSK","invalid-token");invalid.setVersion(0);invalid.setPath("/api/v1/kiosk");cookies.getCookieStore().add(URI.create("http://localhost:"+port),invalid);
  assertStatus(401,call("GET","/kiosk/session",null,null,null,null));
 }
 @Test void inactiveUserCannotUsePreviouslyIssuedCode() throws Exception {
  String csrf=activate(create());Member m=member("active");String code=issue(m,"code").path("accessCode").asText();
  db.update("update users set status='disabled' where id=?",m.user());
  assertEquals("user_inactive",decision("validate-code","accessCode",code,csrf,UUID.randomUUID().toString()).path("reasonCode").asText());
 }
 @Test void otherModulesAndRefreshRemainReachable() throws Exception {
  var refreshed=call("POST","/auth/refresh",Map.of("refreshToken",ownerRefresh),null,null,null);assertStatus(200,refreshed);
  assertStatus(401,call("POST","/auth/refresh",Map.of("refreshToken",ownerRefresh),null,null,null));
  ownerToken=body(refreshed).path("token").asText();assertStatus(200,call("GET","/auth/me",null,ownerToken,null,null));
  db.update("update gym_modules set enabled=true where gym_id=?",gym);
  String root="/gyms/"+gym;
  for(String route:List.of("/staff","/members","/plans","/payments","/products","/sales","/routines","/classes","/class-templates","/dashboard/summary","/landing-blocks","/landing/link","/media-assets","/settings","/modules"))assertStatus(200,call("GET",root+route,null,ownerToken,null,null));
  Member m=member("active");assertStatus(200,call("GET",root+"/reservations/me",null,m.token(),null,null));
  assertStatus(200,call("GET",root+"/members/me",null,m.token(),null,null));
  assertStatus(404,call("GET","/public/media/"+UUID.randomUUID(),null,null,null,null));
  assertStatus(404,call("GET",root+"/media-assets/"+UUID.randomUUID()+"/content",null,ownerToken,null,null));
  db.update("update gym_settings set landing_published=true where gym_id=?",gym);
  assertStatus(200,call("GET","/public/gyms/"+slug+"/landing",null,null,null,null));
 }
 @Test @EnabledIfEnvironmentVariable(named="GMS_TEST_BROWSER_FRONTEND",matches=".+")
 void chromeRunsActualFrontendActivationAndReload() throws Exception {
  var builder=new ProcessBuilder("node","tests/checkin-browser.mjs").directory(new java.io.File(System.getenv("GMS_TEST_BROWSER_FRONTEND"))).redirectErrorStream(true);
  builder.environment().put("GMS_TEST_API_URL","http://localhost:"+port+"/api/v1");
  builder.environment().put("GMS_TEST_OWNER_TOKEN",ownerToken);builder.environment().put("GMS_TEST_GYM",gym.toString());builder.environment().put("GMS_TEST_SLUG",slug);
  var process=builder.start();assertTrue(process.waitFor(55,TimeUnit.SECONDS),"Chrome test timed out");
  String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
  // Runner outputs only sanitized status and no credentials or full DOM.
  assertEquals(0,process.exitValue(),output);assertTrue(output.contains("BROWSER_PASS"),output);
  System.out.println(output);
 }
 @Test void gymCrudAndAccountRegistrationKeepTheirPermissions() throws Exception {
  String root="/gyms/"+gym;UUID owner=db.queryForObject("select user_id from gym_staff_roles where gym_id=? and role='owner'",UUID.class,gym);
  assertStatus(403,call("GET","/gyms",null,ownerToken,null,null));
  var staff=call("POST",root+"/staff/register",Map.of("role","trainer","user",Map.of("fullName","Audit Trainer","email","audit-"+UUID.randomUUID()+"@example.test","password","audit-password")),ownerToken,null,null);
  assertStatus(201,staff);String trainer=jwt(UUID.fromString(body(staff).path("user").path("id").asText()));
  assertStatus(200,call("GET",memberPath()+"/stats",null,trainer,null,null));
  assertStatus(403,call("POST",root+"/checkin-devices",Map.of("deviceName","Forbidden trainer kiosk"),trainer,null,null));
  var member=call("POST",root+"/members/register",Map.of("user",Map.of("fullName","Registered Audit Member","email","audit-"+UUID.randomUUID()+"@example.test","password","audit-password"),"member",Map.of("code","AUD-"+UUID.randomUUID(),"active",true)),ownerToken,null,null);assertStatus(201,member);
  db.update("insert into platform_admins(user_id) values (?)",owner);
  assertStatus(200,call("GET","/gyms",null,ownerToken,null,null));
  var created=call("POST","/gyms",Map.of("name","Audit CRUD "+UUID.randomUUID()),ownerToken,null,null);assertStatus(201,created);String path="/gyms/"+body(created).path("id").asText();
  assertStatus(200,call("GET",path,null,ownerToken,null,null));
  assertStatus(200,call("PATCH",path,Map.of("name","Audit renamed"),ownerToken,null,null));
  assertStatus(204,call("DELETE",path,null,ownerToken,null,null));
  assertStatus(404,call("GET",path,null,ownerToken,null,null));
 }
 @Test void paymentsPosRoutinesAndBookingKeepTheirWriteFlows() throws Exception {
  db.update("update gym_modules set enabled=true where gym_id=?",gym);String root="/gyms/"+gym;
  Member m=member("no_active_subscription");
  var plan=call("POST",root+"/plans",Map.of("name","Audit monthly","billingPeriod","monthly","intervalCount",1,"price",100,"isActive",true),ownerToken,null,null);assertStatus(201,plan);String planId=body(plan).path("id").asText();
  var payment=call("POST",root+"/payments",Map.of("memberId",m.id(),"planId",planId,"method","cash","periods",1),ownerToken,null,null);assertStatus(201,payment);assertEquals(100,body(payment).path("amount").asInt());
  var product=call("POST",root+"/products",Map.of("sku","AUD-"+UUID.randomUUID(),"name","Audit water","price",20,"stock",5,"isActive",true),ownerToken,null,null);assertStatus(201,product);String productId=body(product).path("id").asText();
  var sale=call("POST",root+"/sales",Map.of("items",List.of(Map.of("productId",productId,"quantity",2))),ownerToken,null,null);assertStatus(201,sale);assertEquals(40,body(sale).path("total").asInt());
  assertEquals(3,body(call("GET",root+"/products/"+productId,null,ownerToken,null,null)).path("stock").asInt());
  var routine=call("POST",root+"/routines",Map.of("name","Audit routine","published",true,"days",List.of()),ownerToken,null,null);assertStatus(201,routine);
  var clazz=call("POST",root+"/classes",Map.of("name","Audit class","category","normal","startsAt",OffsetDateTime.now().plusDays(1).toString(),"endsAt",OffsetDateTime.now().plusDays(1).plusHours(1).toString(),"capacity",5),ownerToken,null,null);assertStatus(201,clazz);String classId=body(clazz).path("id").asText();
  assertStatus(200,call("GET",root+"/classes/"+classId+"/availability",null,m.token(),null,null));
  var booked=call("POST",root+"/reservations",Map.of("classId",classId),m.token(),null,null);assertStatus(201,booked);
  assertStatus(204,call("DELETE",root+"/reservations/"+body(booked).path("id").asText(),null,m.token(),null,null));
 }
}
