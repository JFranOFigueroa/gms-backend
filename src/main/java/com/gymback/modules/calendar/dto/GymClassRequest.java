package com.gymback.modules.calendar.dto;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
/** DTO de escritura: solo campos permitidos, excluye id/gymId/createdBy y auditoría. */
public record GymClassRequest(UUID templateId, String name, String category, String color, OffsetDateTime startsAt, OffsetDateTime endsAt, Integer capacity, UUID trainerUserId, String status) {}
