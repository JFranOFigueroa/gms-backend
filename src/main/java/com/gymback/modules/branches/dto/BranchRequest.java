package com.gymback.modules.branches.dto;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
/** DTO de escritura: solo campos permitidos, excluye id/branchId/createdBy y auditoría. */
public record BranchRequest(String name, String shortName, String code, String address, String phone, String logoUrl, String timezone, String currency, String status) {}
