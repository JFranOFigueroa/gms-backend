package com.gymback.modules.checkin.services;
import com.gymback.modules.checkin.dto.CheckinQrDtos.*;
import com.gymback.modules.checkin.interfaces.*;
import com.gymback.modules.checkin.model.*;
import com.gymback.security.*;
import com.gymback.shared.exceptions.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
public class KioskDeviceService {
 private final ICheckinDeviceRepository devices;
 private final ICheckinDeviceSessionRepository sessions;
 private final ICheckinQrTokenRepository qrTokens;
 private final ICheckinContextRepository context;
 private final IIdentityRepository identity;
 private final AuthorizationService auth;
 private final CheckinCrypto crypto;
 private final ActivationAbuseService abuse;
 public KioskDeviceService(ICheckinDeviceRepository devices,ICheckinDeviceSessionRepository sessions,ICheckinQrTokenRepository qrTokens,
  ICheckinContextRepository context,IIdentityRepository identity,AuthorizationService auth,CheckinCrypto crypto,ActivationAbuseService abuse){
  this.devices=devices;this.sessions=sessions;this.qrTokens=qrTokens;this.context=context;this.identity=identity;this.auth=auth;this.crypto=crypto;this.abuse=abuse;
 }
 private void requireAdmin(UUID branch){auth.requireModule(branch,"checkin","owner");}
 @Transactional(readOnly=true)public List<DeviceView> list(UUID branch){requireAdmin(branch);return devices.list(branch).stream().map(this::view).toList();}
 private DeviceView view(CheckinDevice d){return new DeviceView(d.id,d.deviceName,Boolean.TRUE.equals(d.active),d.activatedAt!=null,d.lastSeenAt);}
 private CheckinDevice require(UUID branch,UUID id){return devices.lockInGym(branch,id).orElseThrow(()->new NotFoundException("Dispositivo no encontrado"));}
 private String activation(CheckinDevice d){
  String code=crypto.newToken();d.deviceSecretHash=crypto.hash(code);d.activationExpiresAt=OffsetDateTime.now().plusMinutes(10);d.activatedAt=null;
  return code;
 }
 @Transactional public DeviceCreated create(UUID branch,CreateDevice req){
  requireAdmin(branch);String name=req.deviceName().trim();
  if(name.isEmpty()||name.length()>100)throw new DomainException("Nombre de dispositivo inválido");
  if(devices.byName(branch,name).isPresent())throw new ConflictException("device_exists","Ya existe un dispositivo con este nombre");
  CheckinDevice d=new CheckinDevice();d.branchId=branch;d.deviceName=name;d.createdBy=auth.actor().id();
  String code=activation(d);devices.save(d);
  return new DeviceCreated(d.id,name,true,code,d.activationExpiresAt);
 }
 @Transactional public DeviceCreated reissue(UUID branch,UUID id){
  requireAdmin(branch);CheckinDevice d=require(branch,id);
  if(!Boolean.TRUE.equals(d.active))throw new DomainException("Activa el dispositivo antes de emitir un código");
  sessions.revokeForDevice(d.id);qrTokens.revokeForDevice(d.id);
  String code=activation(d);devices.save(d);
  return new DeviceCreated(d.id,d.deviceName,true,code,d.activationExpiresAt);
 }
 @Transactional public DeviceView change(UUID branch,UUID id,UpdateDevice data){
  requireAdmin(branch);if(data.active()==null)throw new DomainException("Estado de dispositivo obligatorio");
  CheckinDevice d=require(branch,id);
  if(!Boolean.TRUE.equals(data.active())){
   d.active=false;d.activatedAt=null;d.activationExpiresAt=null;
   d.deviceSecretHash=crypto.hash(crypto.newToken());sessions.revokeForDevice(d.id);qrTokens.revokeForDevice(d.id);
  }else{d.active=true;}
  devices.save(d);return view(d);
 }
 @Transactional public void delete(UUID branch,UUID id){
  requireAdmin(branch);CheckinDevice d=require(branch,id);
  sessions.revokeForDevice(d.id);qrTokens.revokeForDevice(d.id);
  d.active=false;d.deletedAt=OffsetDateTime.now();d.deletedBy=auth.actor().id();devices.save(d);
 }
 public record ActivationSession(String sessionToken,OffsetDateTime expiresAt){}
 @Transactional public ActivationSession activate(Activate req,String ip){
  String client=crypto.hash("activation:"+(ip==null?"unknown":ip));
  abuse.check(client);
  var branch=context.byCode(req.code()).orElse(null);
  CheckinDevice device=req.activationCode().length()<=256?devices.byActivationHash(crypto.hash(req.activationCode()),true).orElse(null):null;
  if(branch==null||device==null||!branch.id().equals(device.branchId)||!Boolean.TRUE.equals(device.active)
     ||device.activatedAt!=null||device.activationExpiresAt==null||!device.activationExpiresAt.isAfter(OffsetDateTime.now())
     ||!identity.isModuleEnabled(branch.id(),"checkin")){
   abuse.recordFailure(client);throw new AccessDeniedException("Código de activación inválido");
  }
  // La activación y la sesión quedan en la misma transacción. Un mismo código no se reutiliza.
  device.activatedAt=OffsetDateTime.now();device.activationExpiresAt=null;
  device.deviceSecretHash=crypto.hash(crypto.newToken());devices.save(device);
  String secret=crypto.newToken();CheckinDeviceSession session=new CheckinDeviceSession();
  session.branchId=device.branchId;session.deviceId=device.id;session.sessionTokenHash=crypto.hash(secret);
  session.expiresAt=OffsetDateTime.now().plusHours(12);sessions.save(session);
  return new ActivationSession(secret,session.expiresAt);
 }
 @Transactional public void revokeSession(KioskPrincipal principal){
  sessions.byHash(crypto.hash(principal.sessionSecret())).ifPresent(sessions::revoke);
 }
 @Transactional(readOnly=true)public KioskSession session(KioskPrincipal principal,String csrf){
  var branch=context.byId(principal.branchId()).orElseThrow(()->new AccessDeniedException("Sucursal inactivo"));
  return new KioskSession(branch.name(),branch.code(),principal.deviceName(),principal.expiresAt(),csrf);
 }
 @Transactional(readOnly=true)public PublicConfig publicConfig(String code){
  var branch=context.byCode(code).orElseThrow(()->new NotFoundException("Sucursal no disponible"));
  return new PublicConfig(branch.name(),branch.code(),identity.isModuleEnabled(branch.id(),"checkin"));
 }
}
