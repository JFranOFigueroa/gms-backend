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
 private void requireAdmin(UUID gym){auth.requireModule(gym,"checkin","owner");}
 @Transactional(readOnly=true)public List<DeviceView> list(UUID gym){requireAdmin(gym);return devices.list(gym).stream().map(this::view).toList();}
 private DeviceView view(CheckinDevice d){return new DeviceView(d.id,d.deviceName,Boolean.TRUE.equals(d.active),d.activatedAt!=null,d.lastSeenAt);}
 private CheckinDevice require(UUID gym,UUID id){return devices.lockInGym(gym,id).orElseThrow(()->new NotFoundException("Dispositivo no encontrado"));}
 private String activation(CheckinDevice d){
  String code=crypto.newToken();d.deviceSecretHash=crypto.hash(code);d.activationExpiresAt=OffsetDateTime.now().plusMinutes(10);d.activatedAt=null;
  return code;
 }
 @Transactional public DeviceCreated create(UUID gym,CreateDevice req){
  requireAdmin(gym);String name=req.deviceName().trim();
  if(name.isEmpty()||name.length()>100)throw new DomainException("Nombre de dispositivo inválido");
  if(devices.byName(gym,name).isPresent())throw new ConflictException("device_exists","Ya existe un dispositivo con este nombre");
  CheckinDevice d=new CheckinDevice();d.gymId=gym;d.deviceName=name;d.createdBy=auth.actor().id();
  String code=activation(d);devices.save(d);
  return new DeviceCreated(d.id,name,true,code,d.activationExpiresAt);
 }
 @Transactional public DeviceCreated reissue(UUID gym,UUID id){
  requireAdmin(gym);CheckinDevice d=require(gym,id);
  if(!Boolean.TRUE.equals(d.active))throw new DomainException("Activa el dispositivo antes de emitir un código");
  sessions.revokeForDevice(d.id);qrTokens.revokeForDevice(d.id);
  String code=activation(d);devices.save(d);
  return new DeviceCreated(d.id,d.deviceName,true,code,d.activationExpiresAt);
 }
 @Transactional public DeviceView change(UUID gym,UUID id,UpdateDevice data){
  requireAdmin(gym);if(data.active()==null)throw new DomainException("Estado de dispositivo obligatorio");
  CheckinDevice d=require(gym,id);
  if(!Boolean.TRUE.equals(data.active())){
   d.active=false;d.activatedAt=null;d.activationExpiresAt=null;
   d.deviceSecretHash=crypto.hash(crypto.newToken());sessions.revokeForDevice(d.id);qrTokens.revokeForDevice(d.id);
  }else{d.active=true;}
  devices.save(d);return view(d);
 }
 @Transactional public void delete(UUID gym,UUID id){
  requireAdmin(gym);CheckinDevice d=require(gym,id);
  sessions.revokeForDevice(d.id);qrTokens.revokeForDevice(d.id);
  d.active=false;d.deletedAt=OffsetDateTime.now();d.deletedBy=auth.actor().id();devices.save(d);
 }
 public record ActivationSession(String sessionToken,OffsetDateTime expiresAt){}
 @Transactional public ActivationSession activate(Activate req,String ip){
  String client=crypto.hash("activation:"+(ip==null?"unknown":ip));
  abuse.check(client);
  var gym=context.bySlug(req.slug()).orElse(null);
  CheckinDevice device=req.activationCode().length()<=256?devices.byActivationHash(crypto.hash(req.activationCode()),true).orElse(null):null;
  if(gym==null||device==null||!gym.id().equals(device.gymId)||!Boolean.TRUE.equals(device.active)
     ||device.activatedAt!=null||device.activationExpiresAt==null||!device.activationExpiresAt.isAfter(OffsetDateTime.now())
     ||!identity.isModuleEnabled(gym.id(),"checkin")){
   abuse.recordFailure(client);throw new AccessDeniedException("Código de activación inválido");
  }
  // La activación y la sesión quedan en la misma transacción. Un mismo código no se reutiliza.
  device.activatedAt=OffsetDateTime.now();device.activationExpiresAt=null;
  device.deviceSecretHash=crypto.hash(crypto.newToken());devices.save(device);
  String secret=crypto.newToken();CheckinDeviceSession session=new CheckinDeviceSession();
  session.gymId=device.gymId;session.deviceId=device.id;session.sessionTokenHash=crypto.hash(secret);
  session.expiresAt=OffsetDateTime.now().plusHours(12);sessions.save(session);
  return new ActivationSession(secret,session.expiresAt);
 }
 @Transactional public void revokeSession(KioskPrincipal principal){
  sessions.byHash(crypto.hash(principal.sessionSecret())).ifPresent(sessions::revoke);
 }
 @Transactional(readOnly=true)public KioskSession session(KioskPrincipal principal,String csrf){
  var gym=context.byId(principal.gymId()).orElseThrow(()->new AccessDeniedException("Gimnasio inactivo"));
  return new KioskSession(gym.name(),gym.slug(),principal.deviceName(),principal.expiresAt(),csrf);
 }
 @Transactional(readOnly=true)public PublicConfig publicConfig(String slug){
  var gym=context.bySlug(slug).orElseThrow(()->new NotFoundException("Gimnasio no disponible"));
  return new PublicConfig(gym.name(),gym.slug(),identity.isModuleEnabled(gym.id(),"checkin"));
 }
}
