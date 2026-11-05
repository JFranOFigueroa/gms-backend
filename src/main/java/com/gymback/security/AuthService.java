package com.gymback.security;
import com.gymback.security.dto.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AuthService {
  private static final long ACCESS_SECONDS=3600;
  private static final long REFRESH_SECONDS=30L*24*3600;
  private final SecureRandom secureRandom=new SecureRandom();
  private final IIdentityRepository repo;
  private final IRefreshTokenRepository sessions;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final AuthorizationService authorization;
  private final com.gymback.shared.services.AuditContext auditContext;
  public AuthService(IIdentityRepository repo, IRefreshTokenRepository sessions, PasswordEncoder passwords,
                     JwtEncoder encoder, AuthorizationService authorization,
                     com.gymback.shared.services.AuditContext auditContext) {
    this.repo=repo;this.sessions=sessions;this.passwords=passwords;this.encoder=encoder;this.authorization=authorization;this.auditContext=auditContext;
  }
  @Transactional
  public LoginResponse login(String email, String password) {
    var user=repo.findCredentialsByEmail(email.strip())
      .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
    if (!"active".equals(user.status()) || user.passwordHash()==null
        || !passwords.matches(password,user.passwordHash()))
      throw new BadCredentialsException("Credenciales inválidas");
    auditContext.setActor(user.id());
    return issueSession(user);
  }
  /** Token de un solo uso; el bloqueo SELECT FOR UPDATE evita dos rotaciones concurrentes. */
  @Transactional
  public LoginResponse refresh(String supplied) {
    String hash=tokenHash(supplied);
    var session=sessions.lockValid(hash,Instant.now())
      .orElseThrow(()->new BadCredentialsException("Sesión expirada o revocada"));
    var user=repo.findCredentialsById(session.userId())
      .filter(u->"active".equals(u.status()))
      .orElseThrow(()->new BadCredentialsException("Sesión inválida"));
    auditContext.setActor(user.id());
    sessions.revoke(session.id());
    return issueSession(user);
  }
  @Transactional
  public void logout(String supplied) {
    // Revocación idempotente; un token que no exista no revela información.
    String hash=tokenHash(supplied);
    sessions.lockValid(hash,Instant.now()).ifPresent(session->auditContext.setActor(session.userId()));
    sessions.revokeHash(hash);
  }
  private LoginResponse issueSession(IIdentityRepository.UserCredentials user) {
    Instant now=Instant.now();
    JwtClaimsSet claims=JwtClaimsSet.builder().subject(user.id().toString())
      .issuedAt(now).expiresAt(now.plusSeconds(ACCESS_SECONDS)).issuer("gym-back").build();
    String token=encoder.encode(JwtEncoderParameters.from(
      JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),claims)).getTokenValue();
    byte[] bytes=new byte[48];secureRandom.nextBytes(bytes);
    String refresh=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    sessions.insert(user.id(),tokenHash(refresh),now.plusSeconds(REFRESH_SECONDS));
    AuthSession session=buildSession(user);
    return new LoginResponse(token,"Bearer",session.user(),session.gyms(),refresh,ACCESS_SECONDS);
  }
  private String tokenHash(String token) {
    if(token==null || token.length()<40 || token.length()>512)throw new BadCredentialsException("Refresh token inválido");
    try {
      byte[] digest=MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
  }
  @Transactional(readOnly=true)
  public AuthSession me() {
    Actor actor=authorization.actor();
    var user=repo.findCredentialsById(actor.id())
      .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Usuario no encontrado"));
    return buildSession(user);
  }
  private AuthSession buildSession(IIdentityRepository.UserCredentials user) {
    boolean admin=repo.isPlatformAdmin(user.id());
    List<GymAccess> gyms=repo.gymsForUser(user.id(),admin);
    var defaultGym=gyms.stream().anyMatch(g -> g.id().equals(user.defaultGymId()))
      ? user.defaultGymId() : null;
    return new AuthSession(new AuthUser(user.id(),user.name(),user.email(),admin,defaultGym),gyms);
  }
}
