package com.gymback.security.dto;
import java.util.List;
import java.util.UUID;
/** Una entrada del selector del login; los roles se calculan por sucursal. */
public record BranchAccess(UUID id, String name, String code, List<String> roles) {
  public BranchAccess { roles = List.copyOf(roles); }
}
