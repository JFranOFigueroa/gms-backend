package com.gymback.modules.pos.services;
import com.gymback.modules.pos.model.Product;
import com.gymback.modules.pos.interfaces.IProductRepository;
import com.gymback.modules.pos.dto.ProductRequest;
import com.gymback.shared.services.*;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.security.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class ProductService extends BaseService<Product> {
  private final AuthorizationService auth;private final IProductRepository productRepo;
  public ProductService(IProductRepository repo,AuditService audit,AuthorizationService auth) {super(repo,audit);this.auth=auth;this.productRepo=repo;}
  @Transactional(readOnly=true) public List<Product> all(UUID gymId,int offset,int limit){ auth.requireModule(gymId,"pos","owner","trainer"); return list(gymId,offset,limit); }
  @Transactional(readOnly=true) public Product one(UUID gymId,UUID id){ auth.requireModule(gymId,"pos","owner","trainer"); return getOr404(id,gymId); }
  @Transactional public Product add(UUID gymId,ProductRequest req){
    Actor actor=auth.requireModule(gymId,"pos","owner","trainer");
    if(req.sku()==null || req.sku().isBlank()) throw new DomainException("sku es obligatorio");if(req.name()==null || req.name().isBlank()) throw new DomainException("name es obligatorio");if(req.price()==null) throw new DomainException("price es obligatorio");
    Product e=new Product();
    e.setGymId(gymId);
    if(req.sku()!=null) e.setSku(req.sku()); if(req.barcode()!=null) e.setBarcode(req.barcode()); if(req.name()!=null) e.setName(req.name()); if(req.category()!=null) e.setCategory(req.category()); if(req.price()!=null) e.setPrice(req.price()); if(req.stock()!=null) e.setStock(req.stock()); if(req.lowStockThreshold()!=null) e.setLowStockThreshold(req.lowStockThreshold()); if(req.isActive()!=null) e.setIsActive(req.isActive());

    validate(e);
    Product saved=create(e,actor.id());
    if(saved.getStock()>0)productRepo.recordStockAdjustment(gymId,saved.getId(),saved.getStock(),actor.id());
    return saved;
  }
  @Transactional public Product change(UUID gymId,UUID id,ProductRequest req){
    Actor actor=auth.requireModule(gymId,"pos","owner","trainer");
    Product e=productRepo.lockForEdit(gymId,id).orElseThrow(()->new com.gymback.shared.exceptions.NotFoundException("Producto no encontrado"));
    int previousStock=e.getStock();
    if(req.sku()!=null) e.setSku(req.sku()); if(req.barcode()!=null) e.setBarcode(req.barcode()); if(req.name()!=null) e.setName(req.name()); if(req.category()!=null) e.setCategory(req.category()); if(req.price()!=null) e.setPrice(req.price()); if(req.stock()!=null) e.setStock(req.stock()); if(req.lowStockThreshold()!=null) e.setLowStockThreshold(req.lowStockThreshold()); if(req.isActive()!=null) e.setIsActive(req.isActive());
    validate(e);
    Product saved=update(e,actor.id());
    int delta=saved.getStock()-previousStock;
    if(delta!=0)productRepo.recordStockAdjustment(gymId,id,delta,actor.id());
    return saved;
  }
  @Transactional public void remove(UUID gymId,UUID id){
    Actor actor=auth.requireModule(gymId,"pos","owner","trainer");
    Product e=productRepo.lockForEdit(gymId,id).orElseThrow(()->new com.gymback.shared.exceptions.NotFoundException("Producto no encontrado"));
    e.setIsActive(false);softDelete(e,actor.id());
  }
  private void validate(Product e){ if(e.getStock()==null || e.getStock()<0 || e.getPrice()==null || e.getPrice().signum()<0 || e.getLowStockThreshold()==null || e.getLowStockThreshold()<0)throw new DomainException("Precio o inventario inválido"); }
}
