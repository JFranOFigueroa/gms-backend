package com.gymback.modules.staff.dto;
import java.util.UUID;
/** POST asocia usuario global ya activo; PATCH recibe solo role. */
public record StaffRequest(UUID userId, String role) {}
