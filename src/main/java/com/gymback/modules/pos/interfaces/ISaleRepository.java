package com.gymback.modules.pos.interfaces;
import com.gymback.modules.pos.dto.SaleView.SaleItemView;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public interface ISaleRepository {
  record ProductSnapshot(UUID id,String name,BigDecimal price,int stock) {}
  record SaleSnapshot(UUID id,UUID gymId,BigDecimal total,String currency,String status,
                      UUID soldBy,OffsetDateTime soldAt) {}
  Optional<ProductSnapshot> lockProduct(UUID gym,UUID product);
  UUID createSale(UUID gym,BigDecimal amount,UUID actor);
  void addSaleItem(UUID gym,UUID sale,ProductSnapshot product,int quantity);
  void reduceStock(UUID gym,UUID product,int qty);
  void inventoryMovement(UUID gym,UUID product,int delta,UUID sale,UUID actor);
  List<SaleSnapshot> sales(UUID gym,int limit);
  Optional<SaleSnapshot> sale(UUID gym,UUID saleId);
  /** Devuelve ítems de todas las ventas en una consulta, no N+1. */
  Map<UUID,List<SaleItemView>> items(UUID gym,Collection<UUID> saleIds);
}
