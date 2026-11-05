package com.gymback.modules.pos.interfaces;
import com.gymback.shared.interfaces.IBaseCrudRepository;
import com.gymback.modules.pos.model.Product;
public interface IProductRepository extends IBaseCrudRepository<Product> {
 java.util.Optional<Product> lockForEdit(java.util.UUID gym,java.util.UUID product);
 void recordStockAdjustment(java.util.UUID gym,java.util.UUID product,int delta,java.util.UUID actor);
}
