package com.gymback.modules.adminplans.dto;

import java.math.BigDecimal;

/** Solo datos editables: id, branchId y auditoría los determina el backend. */
public record AdminPlanRequest(
    String name,
    String description,
    BigDecimal price,
    Integer durationDays,
    Boolean isActive
) {}
