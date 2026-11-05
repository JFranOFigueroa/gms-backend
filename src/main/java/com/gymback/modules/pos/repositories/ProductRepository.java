package com.gymback.modules.pos.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.pos.model.Product;
import com.gymback.modules.pos.interfaces.IProductRepository;
import com.gymback.shared.persistence.model.InventoryMovement;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
@Repository
public class ProductRepository extends BaseCRUD<Product> implements IProductRepository {
  public ProductRepository(EntityManager em){super(em,Product.class,true);}
  @Override public java.util.Optional<Product> lockForEdit(java.util.UUID gym,java.util.UUID product){
    return em.createQuery("select p from Product p where p.id=:id and p.gymId=:gym",Product.class)
      .setParameter("id",product).setParameter("gym",gym).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream().findFirst();
  }
  @Override public void recordStockAdjustment(java.util.UUID gym,java.util.UUID product,int delta,java.util.UUID actor){
    if(delta==0)return;
    InventoryMovement m=new InventoryMovement();m.gymId=gym;m.productId=product;m.stockDelta=delta;
    m.reason="adjustment";m.performedBy=actor;em.persist(m);em.flush();
  }
}
