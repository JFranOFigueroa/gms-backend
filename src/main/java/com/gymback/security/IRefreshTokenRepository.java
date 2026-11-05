package com.gymback.security;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
/** Persistencia de sesiones; se almacena únicamente SHA-256 del secreto aleatorio. */
public interface IRefreshTokenRepository {
  record Session(UUID id, UUID userId, Instant expiresAt) {}
  Optional<Session> lockValid(String hash,Instant now);
  void insert(UUID userId,String hash,Instant expiresAt);
  void revoke(UUID sessionId);
  void revokeHash(String hash);
}
