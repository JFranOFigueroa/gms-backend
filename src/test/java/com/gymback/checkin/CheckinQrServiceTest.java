package com.gymback.checkin;
import com.gymback.modules.checkin.interfaces.*;
import com.gymback.modules.checkin.model.*;
import com.gymback.modules.checkin.services.*;
import com.gymback.security.KioskPrincipal;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CheckinQrServiceTest {
 @Test void issuedGymQrIsHashedAndExpiresAtServerTtl(){
  var tokens=mock(ICheckinQrTokenRepository.class);var devices=mock(ICheckinDeviceRepository.class);
  var context=mock(ICheckinContextRepository.class);
  var crypto=new CheckinCrypto("separate-member-long-secret-for-test-111","separate-csrf-long-secret-for-test-222");
  var service=new CheckinQrService(tokens,devices,context,crypto);
  UUID gym=UUID.randomUUID(),deviceId=UUID.randomUUID();
  var device=new CheckinDevice();device.id=deviceId;device.gymId=gym;device.activatedAt=OffsetDateTime.now();device.active=true;
  when(context.byId(gym)).thenReturn(Optional.of(new ICheckinContextRepository.GymContext(gym,"Gym","gym","America/Mexico_City",10)));
  when(devices.lockInGym(gym,deviceId)).thenReturn(Optional.of(device));
  var result=service.issueGym(new KioskPrincipal(gym,deviceId,UUID.randomUUID(),"Recepción",OffsetDateTime.now().plusHours(1),crypto.newToken()));
  assertEquals(8,result.refreshAfterSeconds());assertEquals(10,result.expiresAt().toEpochSecond()-result.serverTime().toEpochSecond());
  verify(tokens).save(argThat(t->t.tokenHash.equals(crypto.hash(result.qrValue())) && !t.tokenHash.equals(result.qrValue())));
 }
 @Test void expiredGymQrNeverValidates(){
  var tokens=mock(ICheckinQrTokenRepository.class);var devices=mock(ICheckinDeviceRepository.class);
  var context=mock(ICheckinContextRepository.class);
  var crypto=new CheckinCrypto("separate-member-long-secret-for-test-111","separate-csrf-long-secret-for-test-222");
  var service=new CheckinQrService(tokens,devices,context,crypto);
  UUID gym=UUID.randomUUID(),device=UUID.randomUUID();String raw=crypto.newToken();
  var token=new CheckinQrToken();token.gymId=gym;token.deviceId=device;token.expiresAt=OffsetDateTime.now().minusSeconds(1);
  when(tokens.byHash(crypto.hash(raw))).thenReturn(Optional.of(token));
  var result=service.validateGym(raw,gym);
  assertFalse(result.valid());assertEquals("token_expired",result.reason());
 }
}
