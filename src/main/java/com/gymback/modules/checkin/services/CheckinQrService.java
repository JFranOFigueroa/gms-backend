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
 @Transactional public IssuedQr issueBranch(KioskPrincipal kiosk){
  var branch=context.byId(kiosk.branchId()).orElseThrow(()->new AccessDeniedException("Sucursal inactivo"));
  // Bloqueo del dispositivo: dos peticiones paralelas no evaden la cuota.
  var locked=devices.lockInGym(kiosk.branchId(),kiosk.deviceId());
  if(locked.isEmpty()||!Boolean.TRUE.equals(locked.get().active)||locked.get().activatedAt==null)
   throw new AccessDeniedException("Dispositivo no disponible");
  OffsetDateTime now=OffsetDateTime.now();int ttl=branch.qrTtl();
  if(tokens.countIssuedSince(kiosk.deviceId(),now.minusMinutes(1))>=20)throw new RateLimitException(60);
  String raw=crypto.newToken();CheckinQrToken token=new CheckinQrToken();
  token.branchId=kiosk.branchId();token.deviceId=kiosk.deviceId();token.tokenHash=crypto.hash(raw);
  token.issuedAt=now;token.expiresAt=now.plusSeconds(ttl);tokens.save(token);
  return new IssuedQr(raw,token.expiresAt,now,Math.max(3,ttl-2));
 }
 public record BranchTokenState(boolean valid,String reason,UUID deviceId){}
 @Transactional(readOnly=true)public BranchTokenState validateBranch(String raw,UUID branch){
  if(raw==null||raw.isBlank()||raw.length()>256)return new BranchTokenState(false,"invalid_token",null);
  var token=tokens.byHash(crypto.hash(raw));
  if(token.isEmpty())return new BranchTokenState(false,"invalid_token",null);
  CheckinQrToken t=token.get();
  if(!branch.equals(t.branchId))return new BranchTokenState(false,"wrong_gym",null);
  if(!t.expiresAt.isAfter(OffsetDateTime.now()))return new BranchTokenState(false,"token_expired",null);
  var d=devices.findInGym(branch,t.deviceId);
  if(d.isEmpty()||!Boolean.TRUE.equals(d.get().active)||d.get().activatedAt==null)return new BranchTokenState(false,"device_inactive",null);
  return new BranchTokenState(true,"granted",t.deviceId);
 }
 public IssuedQr issueMember(UUID branch,UUID member){
  OffsetDateTime now=OffsetDateTime.now();return new IssuedQr(crypto.memberQr(branch,member,now),now.plusSeconds(30),now,null);
 }
 public CheckinCrypto.MemberClaims verifyMember(String raw,UUID branch){
  CheckinCrypto.MemberClaims claims=crypto.verifyMemberQr(raw,OffsetDateTime.now());
  if(!branch.equals(claims.branch()))throw new DomainException("Credencial de otro sucursal");
  return claims;
 }
}
