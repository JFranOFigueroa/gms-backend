package com.gymback.security;
import com.gymback.security.dto.BranchAccess;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;

class AuthServiceTest {
  @Test void loginAndMeKeepSameMultiBranchIdentityAndRoles() {
    var repo=mock(IIdentityRepository.class);
    var passwords=mock(PasswordEncoder.class);
    var encoder=mock(JwtEncoder.class);
    var auth=mock(AuthorizationService.class);
    UUID userId=UUID.randomUUID(), branchA=UUID.randomUUID(), branchB=UUID.randomUUID();
    var credentials=new IIdentityRepository.UserCredentials(userId,"Ana","ana@example.test","$hash","active",branchA);
    var branches=List.of(new BranchAccess(branchA,"Branch A","branch-a",List.of("owner","trainer")),
                     new BranchAccess(branchB,"Branch B","branch-b",List.of("member")));
    when(repo.findCredentialsByEmail("ana@example.test")).thenReturn(Optional.of(credentials));
    when(repo.findCredentialsById(userId)).thenReturn(Optional.of(credentials));
    when(repo.isPlatformAdmin(userId)).thenReturn(false);
    when(repo.branchesForUser(userId,false)).thenReturn(branches);
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
    verify(sessions).insert(any(UUID.class),eq(userId),anyString(),any(java.time.Instant.class));
    assertEquals(login.user(),me.user());
    assertEquals(login.branches(),me.branches());
    assertEquals(List.of("owner","trainer"),login.branches().get(0).roles());
    assertEquals(List.of("member"),login.branches().get(1).roles());
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
    assertNull(service.me().user().defaultBranchId());
    assertTrue(service.me().branches().isEmpty());
  }
}
