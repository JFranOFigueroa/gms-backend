package com.gymback.modules.media.dto;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
/** DTO de escritura: solo campos permitidos, excluye id/gymId/createdBy y auditoría. */
public record MediaAssetRequest(String name, String mediaType, String externalUrl, String mimeType, Long byteSize) {}
