package com.gymback.modules.landing.dto;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
/** DTO de escritura: solo campos permitidos, excluye id/gymId/createdBy y auditoría. */
public record LandingBlockRequest(String blockType, Integer sortOrder, Boolean enabled, Map<String,Object> blockData) {}
