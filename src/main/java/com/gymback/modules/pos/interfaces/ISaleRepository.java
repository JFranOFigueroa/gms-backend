package com.gymback.modules.pos.interfaces;
import com.gymback.modules.pos.dto.SaleView.SaleItemView;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public interface ISaleRepository {
  record ProductSnapshot(UUID id,String name,BigDecimal price,int stock) {}
  record SaleSnapshot(UUID id,UUID branchId,BigDecimal total,String currency,String status,
                      UUID soldBy,OffsetDateTime soldAt) {}
  Optional<ProductSnapshot> lockProduct(UUID branch,UUID product);
  UUID createSale(UUID branch,BigDecimal amount,UUID actor);
  void addSaleItem(UUID branch,UUID sale,ProductSnapshot product,int quantity);
  void reduceStock(UUID branch,UUID product,int qty);
  void inventoryMovement(UUID branch,UUID product,int delta,UUID sale,UUID actor);
  List<SaleSnapshot> sales(UUID branch,int limit);
  Optional<SaleSnapshot> sale(UUID branch,UUID saleId);
  /** Devuelve ítems de todas las ventas en una consulta, no N+1. */
  Map<UUID,List<SaleItemView>> items(UUID branch,Collection<UUID> saleIds);
}
