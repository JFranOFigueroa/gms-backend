package com.gymback.modules.pos.dto;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
/** DTO de escritura: solo campos permitidos, excluye id/gymId/createdBy y auditoría. */
public record ProductRequest(String sku, String barcode, String name, String category, BigDecimal price, Integer stock, Integer lowStockThreshold, Boolean isActive) {}
