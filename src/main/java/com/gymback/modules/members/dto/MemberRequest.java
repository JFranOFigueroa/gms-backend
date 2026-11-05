package com.gymback.modules.members.dto;
import java.time.LocalDate;
import java.util.UUID;
/**
 * Alta: userId existente/activo en test.users.
 * PATCH: userId debe omitirse; nunca modifica identidad global.
 * No se reciben name,email,phone,password ni se crean cuentas invitadas.
 */
public record MemberRequest(UUID userId, String code, LocalDate birthDate, String address,
  String emergencyContactName, String emergencyContactPhone, UUID photoAssetId,
  String notes, LocalDate startDate, Boolean active) {}
