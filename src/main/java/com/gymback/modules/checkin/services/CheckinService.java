package com.gymback.modules.checkin.services;
import com.gymback.modules.checkin.dto.CheckinRequest;
import com.gymback.modules.checkin.dto.CheckinQrDtos.*;
import com.gymback.modules.checkin.interfaces.*;
import com.gymback.modules.checkin.model.CheckinAccessCode;
import com.gymback.security.*;
import com.gymback.shared.exceptions.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
public class CheckinService {
 private final ICheckinRepository repo;
 private final AuthorizationService auth;
 private final MemberAccessPolicy policy;
 private final CheckinQrService qr;
 private final CheckinCrypto crypto;
 private final ICheckinContextRepository context;
 private final ICheckinAccessCodeRepository accessCodes;
 public CheckinService(ICheckinRepository repo,AuthorizationService auth,MemberAccessPolicy policy,CheckinQrService qr,
                       CheckinCrypto crypto,ICheckinContextRepository context,ICheckinAccessCodeRepository accessCodes){
  this.repo=repo;this.auth=auth;this.policy=policy;this.qr=qr;this.crypto=crypto;this.context=context;this.accessCodes=accessCodes;
 }
 @Transactional(readOnly=true)public List<Map<String,Object>> audit(UUID gym,int limit){auth.requireModule(gym,"checkin","owner","trainer");return repo.list(gym,limit);}
 @Transactional(readOnly=true)public Map<String,Object> own(UUID gym,LocalDate from,LocalDate to,int limit){
  Actor actor=auth.requireModule(gym,"checkin","member");
  if(limit<1||limit>100||from!=null&&to!=null&&from.isAfter(to))throw new DomainException("Rango inválido");
  return Map.of("items",repo.own(gym,actor.id(),from,to,limit));
 }
 /** Conserva 201, JSON y nombre de razón de la versión previa. */
 @Transactional public Map<String,Object> manual(UUID gym,CheckinRequest req){
  auth.requireModule(gym,"checkin","owner","trainer");
  if(req.memberId()==null)throw new DomainException("memberId obligatorio");
  MemberAccessPolicy.Decision d=policy.evaluate(gym,req.memberId());
  String reason="granted".equals(d.reason())?"allowed":d.reason();
  UUID event=repo.record(gym,"not_found".equals(reason)?null:req.memberId(),d.visitDate(),d.allowed(),reason,auth.actor().id());
  return Map.of("eventId",event,"allowed",d.allowed(),"reason",reason,"visitDate",d.visitDate());
 }
 @Transactional(readOnly=true)public IssuedQr myMemberQr(UUID gym){
  Actor actor=auth.requireModule(gym,"checkin","member");
  UUID member=repo.memberIdForUser(gym,actor.id()).orElseThrow(()->new NotFoundException("Membresía no encontrada"));
  return qr.issueMember(gym,member);
 }
 private String idemHash(UUID gym,String actor,String key){
  if(key==null||key.length()>100)throw new DomainException("Idempotency-Key obligatorio");
  try{UUID.fromString(key);}catch(IllegalArgumentException e){throw new DomainException("Idempotency-Key debe ser UUID");}
  return crypto.hash(gym+"|"+actor+"|"+key);
 }
 private CheckinResult result(boolean allowed,String reason,LocalDate date,OffsetDateTime at){
  String message=switch(reason){
   case "granted"->"Acceso registrado correctamente.";
   case "duplicate"->"Ya existe un acceso registrado para hoy.";
   case "expired"->"Tu membresía ha vencido.";
   case "no_payment"->"Consulta en recepción el estado de tu pago.";
   case "member_inactive","user_inactive"->"La membresía se encuentra inactiva.";
   case "no_active_subscription","unknown_validity"->"Consulta en recepción la vigencia de tu membresía.";
   case "token_expired"->"El código QR ha expirado. Solicita uno nuevo.";
   case "invalid_code"->"Código inválido, vencido o ya utilizado. Solicita uno nuevo.";
   default->"No se pudo validar el acceso. Consulta en recepción.";
  };
  return new CheckinResult(allowed,reason,message,at,date);
 }
 private CheckinResult process(UUID gym,UUID member,UUID device,String method,String keyHash,String fingerprint,
                              UUID actor,String qrReason){
  // Con miembro válido se serializa usando SELECT FOR UPDATE; para QR de recepción
  // sin identidad válida se bloquea el dispositivo antes de revisar la idempotencia.
  if(device!=null)repo.lockDevice(gym,device);
  if(member!=null)repo.lockMember(gym,member);
  var prior=repo.priorAttempt(gym,keyHash);
  if(prior.isPresent()){
   var previous=prior.get();
   if(!Objects.equals(previous.fingerprint(),fingerprint))throw new ConflictException("idempotency_conflict","La clave de reintento ya fue utilizada con otros datos");
   return result(previous.allowed(),previous.reason(),previous.visitDate(),previous.occurredAt());
  }
  if(member!=null && repo.countRecent(gym,member,null,OffsetDateTime.now().minusMinutes(1))>=30)
    throw new RateLimitException(60);
  if(device!=null && repo.countRecent(gym,null,device,OffsetDateTime.now().minusMinutes(1))>=120)
    throw new RateLimitException(60);
  MemberAccessPolicy.Decision decision=member!=null?policy.evaluate(gym,member):
    new MemberAccessPolicy.Decision(LocalDate.now(zone(gym)),false,"not_found");
  String reason=qrReason==null?decision.reason():qrReason;
  boolean allowed=qrReason==null&&decision.allowed();
  UUID recordedMember="not_found".equals(decision.reason())?null:member;
  ICheckinRepository.Attempt stored=repo.recordQr(gym,recordedMember,device,method,decision.visitDate(),allowed,reason,actor,keyHash,fingerprint);
  return result(stored.allowed(),stored.reason(),stored.visitDate(),stored.occurredAt());
 }
 private ZoneId zone(UUID gym){
  String tz=context.byId(gym).map(ICheckinContextRepository.GymContext::timezone).orElse("America/Mexico_City");
  return ZoneId.of(tz);
 }
 @Transactional public CheckinResult scanGym(UUID gym,String raw,String key){
  Actor actor=auth.requireModule(gym,"checkin","member");
  UUID member=repo.memberIdForUser(gym,actor.id()).orElseThrow(()->new NotFoundException("Membresía no encontrada"));
  String keyHash=idemHash(gym,"user:"+actor.id(),key);
  String fp=crypto.hash("gym-qr|"+gym+"|"+crypto.hash(raw==null?"":raw));
  var token=qr.validateGym(raw,gym);
  return process(gym,member,token.deviceId(),"kiosk_qr",keyHash,fp,actor.id(),token.valid()?null:token.reason());
 }
 @Transactional public CheckinResult scanMember(KioskPrincipal device,String raw,String key){
  UUID gym=device.gymId();String keyHash=idemHash(gym,"device:"+device.deviceId(),key);
  String fp=crypto.hash("member-qr|"+gym+"|"+crypto.hash(raw==null?"":raw));
  UUID member=null;String error=null;
  try{member=qr.verifyMember(raw,gym).member();}
  catch(DomainException e){error=e.getMessage().toLowerCase(Locale.ROOT).contains("expirada")?"token_expired":
    e.getMessage().contains("otro gimnasio")?"wrong_gym":"invalid_token";}
  return process(gym,member,device.deviceId(),"member_qr",keyHash,fp,null,error);
 }

 /** Emisión para el propio miembro JWT. Nunca se utiliza member_code como credencial. */
 @Transactional public IssuedCode issueManualCode(UUID gym){
  Actor actor=auth.requireModule(gym,"checkin","member");
  UUID member=repo.memberIdForUser(gym,actor.id())
    .orElseThrow(()->new NotFoundException("Membresía no encontrada"));
  // Serializar las emisiones del mismo miembro para invalidar el código anterior.
  if(!repo.lockMember(gym,member))throw new NotFoundException("Membresía no encontrada");
  OffsetDateTime now=OffsetDateTime.now();
  if(accessCodes.issuedSince(gym,member,now.minusMinutes(1))>=3)throw new RateLimitException(60);
  accessCodes.revokeOutstanding(gym,member,now);
  String clearCode=crypto.newAccessCode();
  CheckinAccessCode code=new CheckinAccessCode();
  code.gymId=gym;code.memberId=member;code.codeHash=codeHash(gym,clearCode);
  code.issuedAt=now;code.expiresAt=now.plusMinutes(2);code.createdBy=actor.id();
  accessCodes.save(code);
  return new IssuedCode(clearCode,code.expiresAt,now);
 }
 private String canonicalCode(String raw){
  if(raw==null||raw.length()>24)return "";
  String code=raw.toUpperCase(Locale.ROOT).replace("-","").replace(" ","");
  return code.matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{12}")?code:"";
 }
 private String codeHash(UUID gym,String raw){
  return crypto.hash("checkin-access-code:v1|"+gym+"|"+canonicalCode(raw));
 }
 /** Código manual: solo el kiosco autorizado puede canjearlo, con sesión, origen y CSRF. */
 @Transactional public CheckinResult validateCode(KioskPrincipal device,String raw,String key){
  UUID gym=device.gymId();
  String normalized=canonicalCode(raw);
  String keyHash=idemHash(gym,"device:"+device.deviceId(),key);
  String fingerprint=crypto.hash("manual-code|"+gym+"|"+crypto.hash(normalized.isEmpty()?String.valueOf(raw):normalized));
  // El bloqueo de dispositivo serializa intentos, cuota e idempotencia del mismo kiosco.
  repo.lockDevice(gym,device.deviceId());
  var prior=repo.priorAttempt(gym,keyHash);
  if(prior.isPresent()){
   var p=prior.get();
   if(!Objects.equals(p.fingerprint(),fingerprint))throw new ConflictException("idempotency_conflict","La clave de reintento ya fue utilizada con otros datos");
   return result(p.allowed(),p.reason(),p.visitDate(),p.occurredAt());
  }
  if(repo.countRecentCodeAttempts(gym,device.deviceId(),OffsetDateTime.now().minusMinutes(1))>=10)
   throw new RateLimitException(60);
  UUID member=null;
  if(!normalized.isEmpty()){
   String hash=codeHash(gym,normalized);
   var identified=accessCodes.byHash(gym,hash);
   if(identified.isPresent()){
    // Orden de bloqueos: dispositivo -> miembro -> código. La emisión usa miembro -> código;
    // evita un ciclo de espera al emitir y validar simultáneamente.
    UUID identifiedMember=identified.get().memberId;
    if(repo.lockMember(gym,identifiedMember)){
     var match=accessCodes.lockByHash(gym,hash);
     if(match.isPresent()){
      CheckinAccessCode code=match.get();
      OffsetDateTime now=OffsetDateTime.now();
      if(code.consumedAt==null&&code.revokedAt==null&&code.expiresAt.isAfter(now)){
       member=code.memberId;
       code.consumedAt=now;
       accessCodes.save(code);
      }
     }
    }
   }
  }
  // Un código ausente/vencido/reutilizado se registra como intento denegado, sin datos privados.
  return process(gym,member,device.deviceId(),"kiosk_code",keyHash,fingerprint,null,
     member==null?"invalid_code":null);
 }
 @Transactional(readOnly=true)public Stats stats(UUID gym,LocalDate from,LocalDate to){
  auth.requireModule(gym,"checkin","owner","trainer");
  var contextGym=context.byId(gym).orElseThrow(()->new NotFoundException("Gimnasio no encontrado"));
  LocalDate end=to==null?LocalDate.now(ZoneId.of(contextGym.timezone())):to;
  LocalDate start=from==null?end:from;
  if(start.isAfter(end)||java.time.temporal.ChronoUnit.DAYS.between(start,end)>31)throw new DomainException("Rango máximo de 32 días");
  ZoneId timezone=ZoneId.of(contextGym.timezone());
  long allowed=0,denied=0;long[][] hours=new long[24][2];
  for(var event:repo.eventsForStats(gym,start,end)){
   int hour=event.occurredAt().atZoneSameInstant(timezone).getHour();
   if(event.allowed()){allowed++;hours[hour][0]++;}else{denied++;hours[hour][1]++;}
  }
  List<HourCount> byHour=new ArrayList<>();
  for(int h=0;h<24;h++)if(hours[h][0]+hours[h][1]>0)
    byHour.add(new HourCount(String.format("%02d:00",h),hours[h][0],hours[h][1]));
  return new Stats(start,end,contextGym.timezone(),allowed+denied,allowed,denied,byHour);
 }
}
