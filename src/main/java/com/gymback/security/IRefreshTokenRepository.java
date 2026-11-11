package com.gymback.security;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
/** Persistencia de sesiones; se almacena únicamente SHA-256 del secreto aleatorio. */
public interface IRefreshTokenRepository {
  record Session(UUID id, UUID userId, Instant expiresAt) {}
  Optional<Session> lockValid(String hash,Instant now);
  /** Explicit ID links the access JWT and refresh session without changing response DTOs. */
  void insert(UUID id,UUID userId,String hash,Instant expiresAt);
  /** Legacy insertion retained for internal callers; new tokens always use explicit session ID. */
  void insert(UUID userId,String hash,Instant expiresAt);
  boolean isActive(UUID sessionId,UUID userId,Instant now);
  void revoke(UUID sessionId);
  void revokeHash(String hash);
}
