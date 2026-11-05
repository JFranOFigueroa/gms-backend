package com.gymback.modules.gyms.dto;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
/** DTO de escritura: solo campos permitidos, excluye id/gymId/createdBy y auditoría. */
public record GymRequest(String name, String shortName, String slug, String address, String phone, String logoUrl, String timezone, String currency, String status) {}
