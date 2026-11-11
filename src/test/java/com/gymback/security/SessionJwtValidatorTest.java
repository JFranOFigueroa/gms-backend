package com.gymback.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SessionJwtValidatorTest {
  @Test void logoutRevokesAccessTokenWithoutChangingItsResponseContract(){
    var sessions=mock(IRefreshTokenRepository.class);
    var token=mock(Jwt.class);
    UUID sid=UUID.randomUUID(),user=UUID.randomUUID();
    when(token.getClaimAsString("sid")).thenReturn(sid.toString());
    when(token.getSubject()).thenReturn(user.toString());
    when(sessions.isActive(eq(sid),eq(user),any(Instant.class))).thenReturn(true,false);
    var validator=new SessionJwtValidator(sessions);
    assertFalse(validator.validate(token).hasErrors());
    assertTrue(validator.validate(token).hasErrors());
  }

  @Test void previouslyIssuedTokensWithoutSessionClaimWorkUntilTheirExpiration(){
    var token=mock(Jwt.class);
    assertFalse(new SessionJwtValidator(mock(IRefreshTokenRepository.class)).validate(token).hasErrors());
  }
}
