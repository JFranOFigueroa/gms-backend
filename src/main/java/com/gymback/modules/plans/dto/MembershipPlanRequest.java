package com.gymback.modules.plans.dto;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
/** DTO de escritura: solo campos permitidos, excluye id/gymId/createdBy y auditoría. */
public record MembershipPlanRequest(String name, String billingPeriod, Integer intervalCount, BigDecimal price, String description, Boolean isActive) {}
