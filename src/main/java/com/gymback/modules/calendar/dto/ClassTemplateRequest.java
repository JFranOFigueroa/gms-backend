package com.gymback.modules.calendar.dto;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
/** DTO de escritura: solo campos permitidos, excluye id/gymId/createdBy y auditoría. */
public record ClassTemplateRequest(String name, String category, String color, Integer durationMinutes, Integer capacity, Boolean isActive) {}
