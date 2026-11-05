package com.gymback.security;
import com.gymback.security.dto.GymAccess;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;

class AuthServiceTest {
  @Test void loginAndMeKeepSameMultiGymIdentityAndRoles() {
    var repo=mock(IIdentityRepository.class);
    var passwords=mock(PasswordEncoder.class);
    var encoder=mock(JwtEncoder.class);
    var auth=mock(AuthorizationService.class);
    UUID userId=UUID.randomUUID(), gymA=UUID.randomUUID(), gymB=UUID.randomUUID();
    var credentials=new IIdentityRepository.UserCredentials(userId,"Ana","ana@example.test","$hash","active",gymA);
    var gyms=List.of(new GymAccess(gymA,"Gym A","gym-a",List.of("owner","trainer")),
                     new GymAccess(gymB,"Gym B","gym-b",List.of("member")));
    when(repo.findCredentialsByEmail("ana@example.test")).thenReturn(Optional.of(credentials));
    when(repo.findCredentialsById(userId)).thenReturn(Optional.of(credentials));
    when(repo.isPlatformAdmin(userId)).thenReturn(false);
    when(repo.gymsForUser(userId,false)).thenReturn(gyms);
    when(passwords.matches("pass123456789","$hash")).thenReturn(true);
    Jwt jwt=mock(Jwt.class); when(jwt.getTokenValue()).thenReturn("jwt-signed");
    when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwt);
    when(auth.actor()).thenReturn(new Actor(userId,false));
    var sessions=mock(IRefreshTokenRepository.class);
    var context=mock(com.gymback.shared.services.AuditContext.class);
    var service=new AuthService(repo,sessions,passwords,encoder,auth,context);
    var login=service.login("ana@example.test","pass123456789");
    var me=service.me();
    assertEquals("Bearer",login.tokenType());
    assertEquals("jwt-signed",login.token());
    assertNotNull(login.refreshToken());
    assertEquals(3600L,login.expiresIn());
    verify(sessions).insert(eq(userId),anyString(),any(java.time.Instant.class));
    assertEquals(login.user(),me.user());
    assertEquals(login.gyms(),me.gyms());
    assertEquals(List.of("owner","trainer"),login.gyms().get(0).roles());
    assertEquals(List.of("member"),login.gyms().get(1).roles());
  }
  @Test void defaultGymIsNullWhenAccessWasRemoved() {
    var repo=mock(IIdentityRepository.class);
    UUID user=UUID.randomUUID(), staleGym=UUID.randomUUID();
    var credentials=new IIdentityRepository.UserCredentials(user,"Lia","lia@example.test","hash","active",staleGym);
    when(repo.findCredentialsById(user)).thenReturn(Optional.of(credentials));
    var authorization=mock(AuthorizationService.class);
    when(authorization.actor()).thenReturn(new Actor(user,false));
    var service=new AuthService(repo,mock(IRefreshTokenRepository.class),mock(PasswordEncoder.class),
      mock(JwtEncoder.class),authorization,mock(com.gymback.shared.services.AuditContext.class));
    assertNull(service.me().user().defaultGymId());
    assertTrue(service.me().gyms().isEmpty());
  }
}
