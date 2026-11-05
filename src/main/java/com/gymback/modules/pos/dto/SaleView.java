package com.gymback.modules.pos.dto;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
/** Lectura uniforme de ventas y checkout, incluyendo sus artículos persistidos. */
public record SaleView(UUID id, UUID gymId, BigDecimal total, String currency, String status,
                       UUID soldBy, OffsetDateTime soldAt, List<SaleItemView> items) {
  public SaleView { items=List.copyOf(items); }
  public record SaleItemView(UUID id, UUID productId, String productName, int quantity,
                             BigDecimal unitPrice, BigDecimal lineTotal) {}
}
