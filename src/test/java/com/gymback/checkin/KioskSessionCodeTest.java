package com.gymback.checkin;

import com.gymback.modules.checkin.interfaces.*;
import com.gymback.modules.checkin.services.*;
import com.gymback.security.*;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class KioskSessionCodeTest {
 @Test void sessionIncludesCodeWithoutRemovingExistingFields(){
  var context=mock(ICheckinContextRepository.class);
  UUID branch=UUID.randomUUID();
  when(context.byId(branch)).thenReturn(Optional.of(new ICheckinContextRepository.BranchContext(branch,"Sucursal Centro","sucursal-centro","America/Mexico_City",10)));
  var service=new KioskDeviceService(mock(ICheckinDeviceRepository.class),mock(ICheckinDeviceSessionRepository.class),
   mock(ICheckinQrTokenRepository.class),context,mock(IIdentityRepository.class),mock(AuthorizationService.class),
   new CheckinCrypto("secret-member-qr-very-long-and-distinct-00001","secret-kiosk-csrf-very-long-and-distinct-00002"),
   mock(ActivationAbuseService.class));
  var principal=new KioskPrincipal(branch,UUID.randomUUID(),UUID.randomUUID(),"Acceso lateral",OffsetDateTime.now().plusHours(1),"secret");
  var session=service.session(principal,"csrf");
  assertEquals("sucursal-centro",session.code());assertEquals("Sucursal Centro",session.branchName());
  assertEquals("Acceso lateral",session.deviceName());assertEquals("csrf",session.csrfToken());
 }
}
