package com.gymback.checkin;
import com.gymback.modules.checkin.interfaces.*;
import com.gymback.modules.checkin.model.*;
import com.gymback.modules.checkin.services.CheckinCrypto;
import com.gymback.security.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DeviceAuthorizationServiceTest {
 @Test void expiredOrRevokedOrDisabledSessionCannotAuthenticate(){
  var sessions=mock(ICheckinDeviceSessionRepository.class);var devices=mock(ICheckinDeviceRepository.class);
  var identity=mock(IIdentityRepository.class);
  var crypto=new CheckinCrypto("separate-member-long-secret-for-test-111","separate-csrf-long-secret-for-test-222");
  var service=new DeviceAuthorizationService(sessions,devices,identity,crypto,"https://front.example.test");
  UUID gym=UUID.randomUUID(),deviceId=UUID.randomUUID();String secret=crypto.newToken();
  var session=new CheckinDeviceSession();session.gymId=gym;session.deviceId=deviceId;session.sessionTokenHash=crypto.hash(secret);
  session.expiresAt=OffsetDateTime.now().plusHours(1);
  var device=new CheckinDevice();device.id=deviceId;device.gymId=gym;device.active=true;device.activatedAt=OffsetDateTime.now();
  when(sessions.byHash(crypto.hash(secret))).thenReturn(Optional.of(session));
  when(devices.findInGym(gym,deviceId)).thenReturn(Optional.of(device));
  when(identity.isGymActive(gym)).thenReturn(true);when(identity.isModuleEnabled(gym,"checkin")).thenReturn(true);
  assertEquals(deviceId,service.authenticate(secret).deviceId());
  device.active=false;
  assertThrows(AccessDeniedException.class,()->service.authenticate(secret));
  device.active=true;session.revokedAt=OffsetDateTime.now();
  assertThrows(AccessDeniedException.class,()->service.authenticate(secret));
  session.revokedAt=null;session.expiresAt=OffsetDateTime.now().minusSeconds(1);
  assertThrows(AccessDeniedException.class,()->service.authenticate(secret));
 }
}
