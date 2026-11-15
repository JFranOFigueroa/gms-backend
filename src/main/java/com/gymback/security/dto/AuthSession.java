package com.gymback.security.dto;
import java.util.List;
/** También utilizada para restaurar sesión desde /auth/me. */
public record AuthSession(AuthUser user, List<BranchAccess> branches) {
  public AuthSession { branches = List.copyOf(branches); }
}
