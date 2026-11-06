package com.gymback.checkin;

import com.gymback.modules.checkin.interfaces.*;
import com.gymback.modules.checkin.services.*;
import com.gymback.security.*;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class KioskSessionSlugTest {
 @Test void sessionIncludesSlugWithoutRemovingExistingFields(){
  var context=mock(ICheckinContextRepository.class);
  UUID gym=UUID.randomUUID();
  when(context.byId(gym)).thenReturn(Optional.of(new ICheckinContextRepository.GymContext(gym,"Gimnasio Centro","gimnasio-centro","America/Mexico_City",10)));
  var service=new KioskDeviceService(mock(ICheckinDeviceRepository.class),mock(ICheckinDeviceSessionRepository.class),
   mock(ICheckinQrTokenRepository.class),context,mock(IIdentityRepository.class),mock(AuthorizationService.class),
   new CheckinCrypto("secret-member-qr-very-long-and-distinct-00001","secret-kiosk-csrf-very-long-and-distinct-00002"),
   mock(ActivationAbuseService.class));
  var principal=new KioskPrincipal(gym,UUID.randomUUID(),UUID.randomUUID(),"Acceso lateral",OffsetDateTime.now().plusHours(1),"secret");
  var session=service.session(principal,"csrf");
  assertEquals("gimnasio-centro",session.slug());assertEquals("Gimnasio Centro",session.gymName());
  assertEquals("Acceso lateral",session.deviceName());assertEquals("csrf",session.csrfToken());
 }
}
