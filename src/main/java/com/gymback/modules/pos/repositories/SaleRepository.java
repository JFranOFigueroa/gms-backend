package com.gymback.modules.pos.repositories;
import com.gymback.modules.pos.dto.SaleView.SaleItemView;
import com.gymback.modules.pos.interfaces.ISaleRepository;
import com.gymback.modules.pos.model.Product;
import com.gymback.shared.persistence.model.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.math.*;
import java.util.*;
@Repository
public class SaleRepository implements ISaleRepository {
  private final EntityManager em;
  public SaleRepository(EntityManager em){this.em=em;}
  @Override public Optional<ProductSnapshot> lockProduct(UUID branch,UUID product){
    return em.createQuery("select p from Product p where p.branchId=:branch and p.id=:id and p.isActive=true",Product.class)
      .setParameter("branch",branch).setParameter("id",product).setLockMode(LockModeType.PESSIMISTIC_WRITE)
      .getResultStream().findFirst().map(p->new ProductSnapshot(p.getId(),p.getName(),p.getPrice(),p.getStock()));
  }
  @Override public UUID createSale(UUID branch,BigDecimal total,UUID actor){
    PosSale sale=new PosSale();sale.branchId=branch;sale.total=total;sale.soldBy=actor;
    em.persist(sale);em.flush();return sale.id;
  }
  @Override public void addSaleItem(UUID branch,UUID sale,ProductSnapshot p,int qty){
    PosSaleItem item=new PosSaleItem();item.branchId=branch;item.saleId=sale;item.productId=p.id();
    item.productName=p.name();item.quantity=qty;item.unitPrice=p.price();em.persist(item);
  }
  @Override public void reduceStock(UUID branch,UUID product,int qty){
    Product item=em.createQuery("select p from Product p where p.branchId=:branch and p.id=:id",Product.class)
      .setParameter("branch",branch).setParameter("id",product).getSingleResult();
    if(item.getStock()<qty)throw new com.gymback.shared.exceptions.DomainException("Stock insuficiente");
    item.setStock(item.getStock()-qty);
  }
  @Override public void inventoryMovement(UUID branch,UUID product,int delta,UUID sale,UUID actor){
    InventoryMovement movement=new InventoryMovement();movement.branchId=branch;movement.productId=product;
    movement.stockDelta=delta;movement.saleId=sale;movement.reason="sale";movement.performedBy=actor;em.persist(movement);
  }
  private SaleSnapshot snapshot(PosSale s){return new SaleSnapshot(s.id,s.branchId,s.total,s.currency,s.status,s.soldBy,s.soldAt);}
  @Override public List<SaleSnapshot> sales(UUID branch,int limit){
    if(limit<1||limit>100)throw new IllegalArgumentException("limit 1..100");
    return em.createQuery("select s from PosSale s where s.branchId=:branch order by s.soldAt desc,s.id desc",PosSale.class)
      .setParameter("branch",branch).setMaxResults(limit).getResultStream().map(this::snapshot).toList();
  }
  @Override public Optional<SaleSnapshot> sale(UUID branch,UUID saleId){
    return em.createQuery("select s from PosSale s where s.branchId=:branch and s.id=:id",PosSale.class)
      .setParameter("branch",branch).setParameter("id",saleId).getResultStream().findFirst().map(this::snapshot);
  }
  @Override public Map<UUID,List<SaleItemView>> items(UUID branch,Collection<UUID> saleIds){
    if(saleIds.isEmpty())return Map.of();
    // Una sola consulta para todas las líneas, sin N+1.
    if(!org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly())em.flush();
    List<PosSaleItem> rows=em.createQuery("select i from PosSaleItem i where i.branchId=:branch and i.saleId in :ids order by i.saleId,i.id",PosSaleItem.class)
      .setParameter("branch",branch).setParameter("ids",saleIds).getResultList();
    Map<UUID,List<SaleItemView>> grouped=new HashMap<>();
    for(PosSaleItem i:rows){
      BigDecimal total=i.lineTotal==null?i.unitPrice.multiply(BigDecimal.valueOf(i.quantity)):i.lineTotal;
      grouped.computeIfAbsent(i.saleId,ignored->new ArrayList<>()).add(new SaleItemView(i.id,i.productId,i.productName,i.quantity,i.unitPrice,total));
    }
    return grouped;
  }
}
