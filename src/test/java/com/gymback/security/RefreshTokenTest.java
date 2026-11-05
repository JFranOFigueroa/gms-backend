package com.gymback.security;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
class RefreshTokenTest {
  @Test void refreshRotatesAndRevokesOldSession(){
    var identities=mock(IIdentityRepository.class);
    var sessions=mock(IRefreshTokenRepository.class);
    UUID userId=UUID.randomUUID(),sessionId=UUID.randomUUID();
    var credentials=new IIdentityRepository.UserCredentials(userId,"Eva","eva@test.invalid","hash","active",null);
    when(identities.findCredentialsById(userId)).thenReturn(Optional.of(credentials));
    when(identities.gymsForUser(eq(userId),anyBoolean())).thenReturn(List.of());
    var key=mock(Jwt.class);when(key.getTokenValue()).thenReturn("jwt");
    var enc=mock(JwtEncoder.class);when(enc.encode(any(JwtEncoderParameters.class))).thenReturn(key);
    when(sessions.lockValid(anyString(),any(Instant.class))).thenReturn(Optional.of(
      new IRefreshTokenRepository.Session(sessionId,userId,Instant.now().plusSeconds(100))));
    var auth=new AuthService(identities,sessions,mock(org.springframework.security.crypto.password.PasswordEncoder.class),
      enc,mock(AuthorizationService.class),mock(com.gymback.shared.services.AuditContext.class));
    var result=auth.refresh("x".repeat(64));
    assertNotNull(result.refreshToken());assertEquals("jwt",result.token());
    verify(sessions).revoke(sessionId);
    verify(sessions).insert(eq(userId),anyString(),any(Instant.class));
  }
  @Test void rejectsInvalidOrReusedToken(){
    var repo=mock(IRefreshTokenRepository.class);
    var auth=new AuthService(mock(IIdentityRepository.class),repo,
      mock(org.springframework.security.crypto.password.PasswordEncoder.class),mock(JwtEncoder.class),
      mock(AuthorizationService.class),mock(com.gymback.shared.services.AuditContext.class));
    assertThrows(BadCredentialsException.class,()->auth.refresh("too-short"));
    assertThrows(BadCredentialsException.class,()->auth.refresh("x".repeat(64)));
  }
}
