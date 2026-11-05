package com.gymback.security.dto;
import java.util.List;
import java.util.UUID;
/** Una entrada del selector del login; los roles se calculan por gimnasio. */
public record GymAccess(UUID id, String name, String slug, List<String> roles) {
  public GymAccess { roles = List.copyOf(roles); }
}
