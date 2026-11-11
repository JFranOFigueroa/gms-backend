package com.gymback.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.Instant;
import java.util.UUID;

/** Access JWTs issued after this deployment are revoked as soon as their refresh session is revoked. */
public class SessionJwtValidator implements OAuth2TokenValidator<Jwt> {
  private final IRefreshTokenRepository sessions;
  public SessionJwtValidator(IRefreshTokenRepository sessions){this.sessions=sessions;}
  @Override public OAuth2TokenValidatorResult validate(Jwt token){
    String sid=token.getClaimAsString("sid");
    // Existing already-issued JWTs have no sid; they age out naturally in <= 1h.
    if(sid==null)return OAuth2TokenValidatorResult.success();
    try {
      UUID sessionId=UUID.fromString(sid);
      UUID userId=UUID.fromString(token.getSubject());
      if(sessions.isActive(sessionId,userId,Instant.now()))return OAuth2TokenValidatorResult.success();
    }catch(IllegalArgumentException ignored){/* invalid claim */}
    return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Sesión revocada o expirada",null));
  }
}
