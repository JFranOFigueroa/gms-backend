package com.gymback.security;
import com.gymback.modules.checkin.interfaces.*;
import com.gymback.modules.checkin.model.*;
import com.gymback.modules.checkin.services.CheckinCrypto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.*;
@Service
// El filtro se ejecuta fuera de MVC/OSIV. Mantener abierto el EntityManager
// durante la consulta de sesión y cada revalidación; touch también escribe.
@Transactional
public class DeviceAuthorizationService {
 private final ICheckinDeviceSessionRepository sessions;
 private final ICheckinDeviceRepository devices;
 private final IIdentityRepository identity;
 private final CheckinCrypto crypto;
 private final String trustedOrigin;
 public DeviceAuthorizationService(ICheckinDeviceSessionRepository sessions,ICheckinDeviceRepository devices,IIdentityRepository identity,
   CheckinCrypto crypto,@Value("${app.frontend-origin}")String trustedOrigin){
  this.sessions=sessions;this.devices=devices;this.identity=identity;this.crypto=crypto;this.trustedOrigin=trustedOrigin;
 }
 public KioskPrincipal authenticate(String token){
  if(token==null||token.isBlank()||token.length()>256)throw new AccessDeniedException("Sesión de dispositivo requerida");
  CheckinDeviceSession session=sessions.byHash(crypto.hash(token)).orElseThrow(()->new AccessDeniedException("Sesión de dispositivo inválida"));
  OffsetDateTime now=OffsetDateTime.now();
  if(session.revokedAt!=null||!session.expiresAt.isAfter(now))throw new AccessDeniedException("Sesión de dispositivo expirada");
  CheckinDevice device=devices.findInGym(session.gymId,session.deviceId).orElseThrow(()->new AccessDeniedException("Dispositivo no disponible"));
  if(!Boolean.TRUE.equals(device.active)||device.activatedAt==null||!identity.isGymActive(session.gymId)
      ||!identity.isModuleEnabled(session.gymId,"checkin"))throw new AccessDeniedException("Dispositivo no disponible");
  if(session.lastSeenAt==null||session.lastSeenAt.isBefore(now.minusMinutes(1)))sessions.touch(session.id,device.id,now);
  return new KioskPrincipal(session.gymId,device.id,session.id,device.deviceName,session.expiresAt,token);
 }
 public KioskPrincipal current(){
  Object principal=SecurityContextHolder.getContext().getAuthentication()==null?null:SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  if(!(principal instanceof KioskPrincipal kiosk))throw new AccessDeniedException("Sesión de dispositivo requerida");
  // Revalidación en cada operación, también cuando se desactiva un gimnasio o dispositivo.
  return authenticate(kiosk.sessionSecret());
 }
 public void requireOrigin(HttpServletRequest request){
  if(!trustedOrigin.equals(request.getHeader("Origin")))throw new AccessDeniedException("Origen no autorizado");
 }
 public KioskPrincipal requireMutation(HttpServletRequest request){
  requireOrigin(request);
  KioskPrincipal kiosk=current();
  if(!crypto.matchesCsrf(kiosk.sessionSecret(),request.getHeader("X-CSRF-Token")))throw new AccessDeniedException("CSRF inválido");
  return kiosk;
 }
 public String csrf(KioskPrincipal kiosk){return crypto.csrf(kiosk.sessionSecret());}
}
