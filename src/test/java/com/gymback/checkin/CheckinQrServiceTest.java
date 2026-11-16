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
  UUID branch=UUID.randomUUID(),deviceId=UUID.randomUUID();
  var device=new CheckinDevice();device.id=deviceId;device.branchId=branch;device.activatedAt=OffsetDateTime.now();device.active=true;
  when(context.byId(branch)).thenReturn(Optional.of(new ICheckinContextRepository.BranchContext(branch,"Branch","branch","America/Mexico_City",10)));
  when(devices.lockInGym(branch,deviceId)).thenReturn(Optional.of(device));
  var result=service.issueBranch(new KioskPrincipal(branch,deviceId,UUID.randomUUID(),"Recepción",OffsetDateTime.now().plusHours(1),crypto.newToken()));
  assertEquals(8,result.refreshAfterSeconds());assertEquals(10,result.expiresAt().toEpochSecond()-result.serverTime().toEpochSecond());
  verify(tokens).save(argThat(t->t.tokenHash.equals(crypto.hash(result.qrValue())) && !t.tokenHash.equals(result.qrValue())));
 }
 @Test void expiredGymQrNeverValidates(){
  var tokens=mock(ICheckinQrTokenRepository.class);var devices=mock(ICheckinDeviceRepository.class);
  var context=mock(ICheckinContextRepository.class);
  var crypto=new CheckinCrypto("separate-member-long-secret-for-test-111","separate-csrf-long-secret-for-test-222");
  var service=new CheckinQrService(tokens,devices,context,crypto);
  UUID branch=UUID.randomUUID(),device=UUID.randomUUID();String raw=crypto.newToken();
  var token=new CheckinQrToken();token.branchId=branch;token.deviceId=device;token.expiresAt=OffsetDateTime.now().minusSeconds(1);
  when(tokens.byHash(crypto.hash(raw))).thenReturn(Optional.of(token));
  var result=service.validateBranch(raw,branch);
  assertFalse(result.valid());assertEquals("token_expired",result.reason());
 }
}
