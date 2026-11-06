package com.gymback.modules.checkin.services;
import com.gymback.modules.checkin.dto.CheckinQrDtos.*;
import com.gymback.modules.checkin.interfaces.*;
import com.gymback.modules.checkin.model.*;
import com.gymback.security.KioskPrincipal;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.exceptions.RateLimitException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
public class CheckinQrService {
 private final ICheckinQrTokenRepository tokens;
 private final ICheckinDeviceRepository devices;
 private final ICheckinContextRepository context;
 private final CheckinCrypto crypto;
 public CheckinQrService(ICheckinQrTokenRepository tokens,ICheckinDeviceRepository devices,ICheckinContextRepository context,CheckinCrypto crypto){
  this.tokens=tokens;this.devices=devices;this.context=context;this.crypto=crypto;
 }
 @Transactional public IssuedQr issueGym(KioskPrincipal kiosk){
  var gym=context.byId(kiosk.gymId()).orElseThrow(()->new AccessDeniedException("Gimnasio inactivo"));
  // Bloqueo del dispositivo: dos peticiones paralelas no evaden la cuota.
  var locked=devices.lockInGym(kiosk.gymId(),kiosk.deviceId());
  if(locked.isEmpty()||!Boolean.TRUE.equals(locked.get().active)||locked.get().activatedAt==null)
   throw new AccessDeniedException("Dispositivo no disponible");
  OffsetDateTime now=OffsetDateTime.now();int ttl=gym.qrTtl();
  if(tokens.countIssuedSince(kiosk.deviceId(),now.minusMinutes(1))>=20)throw new RateLimitException(60);
  String raw=crypto.newToken();CheckinQrToken token=new CheckinQrToken();
  token.gymId=kiosk.gymId();token.deviceId=kiosk.deviceId();token.tokenHash=crypto.hash(raw);
  token.issuedAt=now;token.expiresAt=now.plusSeconds(ttl);tokens.save(token);
  return new IssuedQr(raw,token.expiresAt,now,Math.max(3,ttl-2));
 }
 public record GymTokenState(boolean valid,String reason,UUID deviceId){}
 @Transactional(readOnly=true)public GymTokenState validateGym(String raw,UUID gym){
  if(raw==null||raw.isBlank()||raw.length()>256)return new GymTokenState(false,"invalid_token",null);
  var token=tokens.byHash(crypto.hash(raw));
  if(token.isEmpty())return new GymTokenState(false,"invalid_token",null);
  CheckinQrToken t=token.get();
  if(!gym.equals(t.gymId))return new GymTokenState(false,"wrong_gym",null);
  if(!t.expiresAt.isAfter(OffsetDateTime.now()))return new GymTokenState(false,"token_expired",null);
  var d=devices.findInGym(gym,t.deviceId);
  if(d.isEmpty()||!Boolean.TRUE.equals(d.get().active)||d.get().activatedAt==null)return new GymTokenState(false,"device_inactive",null);
  return new GymTokenState(true,"granted",t.deviceId);
 }
 public IssuedQr issueMember(UUID gym,UUID member){
  OffsetDateTime now=OffsetDateTime.now();return new IssuedQr(crypto.memberQr(gym,member,now),now.plusSeconds(30),now,null);
 }
 public CheckinCrypto.MemberClaims verifyMember(String raw,UUID gym){
  CheckinCrypto.MemberClaims claims=crypto.verifyMemberQr(raw,OffsetDateTime.now());
  if(!gym.equals(claims.gym()))throw new DomainException("Credencial de otro gimnasio");
  return claims;
 }
}
