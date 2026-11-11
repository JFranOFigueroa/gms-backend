package com.gymback.modules.checkin.routes;
import com.gymback.modules.checkin.dto.CheckinQrDtos.*;
import com.gymback.modules.checkin.services.*;
import com.gymback.security.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.*;
@RestController @RequestMapping("/api/v1/kiosk")
public class KioskController {
 private final KioskDeviceService devices;private final CheckinQrService qr;
 private final CheckinService checkins;private final DeviceAuthorizationService auth;
 private final boolean secure;
 private final ClientIpResolver clients;
 public KioskController(KioskDeviceService devices,CheckinQrService qr,CheckinService checkins,DeviceAuthorizationService auth,
                         @Value("${app.checkin.cookie-secure:true}")boolean secure,ClientIpResolver clients){
  this.devices=devices;this.qr=qr;this.checkins=checkins;this.auth=auth;this.secure=secure;this.clients=clients;
 }
 private ResponseCookie cookie(String token,Duration life){
  return ResponseCookie.from(DeviceAuthenticationFilter.COOKIE,token).httpOnly(true).secure(secure)
    .sameSite("Strict").path("/api/v1/kiosk").maxAge(life).build();
 }
 @PostMapping("/activate") public ResponseEntity<Void> activate(@Valid @RequestBody Activate request,HttpServletRequest http){
  auth.requireOrigin(http);
  var result=devices.activate(request,clients.resolve(http));
  return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,
    cookie(result.sessionToken(),Duration.between(OffsetDateTime.now(),result.expiresAt())).toString())
    .cacheControl(CacheControl.noStore()).build();
 }
 @GetMapping("/session") public ResponseEntity<KioskSession> session(){
  KioskPrincipal principal=auth.current();
  return ResponseEntity.ok().cacheControl(CacheControl.noStore())
    .body(devices.session(principal,auth.csrf(principal)));
 }
 @PostMapping("/logout") public ResponseEntity<Void> logout(HttpServletRequest http){
  KioskPrincipal principal=auth.requireMutation(http);devices.revokeSession(principal);
  return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,cookie("",Duration.ZERO).toString())
   .cacheControl(CacheControl.noStore()).build();
 }
 @PostMapping("/qr/issue") public ResponseEntity<IssuedQr> issue(HttpServletRequest http){
  KioskPrincipal principal=auth.requireMutation(http);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(qr.issueGym(principal));
 }
 @PostMapping("/checkins/validate-code") public ResponseEntity<CheckinResult> validateCode(
   @Valid @RequestBody ValidateCode request,@RequestHeader("Idempotency-Key")String idempotencyKey,HttpServletRequest http){
  KioskPrincipal principal=auth.requireMutation(http);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore())
   .body(checkins.validateCode(principal,request.accessCode(),idempotencyKey));
 }
 @PostMapping("/checkins/scan-member-qr") public ResponseEntity<CheckinResult> scanMember(
  @Valid @RequestBody ScanMember request,@RequestHeader("Idempotency-Key")String idempotencyKey,HttpServletRequest http){
  KioskPrincipal principal=auth.requireMutation(http);
  return ResponseEntity.ok().cacheControl(CacheControl.noStore())
   .body(checkins.scanMember(principal,request.memberQr(),idempotencyKey));
 }
}
