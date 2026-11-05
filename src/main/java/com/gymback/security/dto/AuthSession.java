package com.gymback.security.dto;
import java.util.List;
/** También utilizada para restaurar sesión desde /auth/me. */
public record AuthSession(AuthUser user, List<GymAccess> gyms) {
  public AuthSession { gyms = List.copyOf(gyms); }
}
