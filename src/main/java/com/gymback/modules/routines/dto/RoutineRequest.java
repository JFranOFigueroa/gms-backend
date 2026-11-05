package com.gymback.modules.routines.dto;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
/** DTO de escritura: solo campos permitidos, excluye id/gymId/createdBy y auditoría. */
public record RoutineRequest(String name, String description, String level, String goal, Boolean published, java.util.List<RoutineDayRequest> days) {}
