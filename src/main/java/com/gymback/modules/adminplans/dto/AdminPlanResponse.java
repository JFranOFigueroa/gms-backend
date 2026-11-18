package com.gymback.modules.adminplans.dto;

import com.gymback.modules.adminplans.model.AdminPlan;
import java.math.BigDecimal;
import java.util.UUID;

/** Contrato de lectura para el frontend; no expone datos internos de auditoría. */
public record AdminPlanResponse(
    UUID id,
    UUID branchId,
    String name,
    String description,
    BigDecimal price,
    Integer durationDays,
    Boolean isActive
) {
  public static AdminPlanResponse from(AdminPlan p) {
    return new AdminPlanResponse(p.getId(), p.getBranchId(), p.getName(),
        p.getDescription(), p.getPrice(), p.getDurationDays(), p.getIsActive());
  }
}
