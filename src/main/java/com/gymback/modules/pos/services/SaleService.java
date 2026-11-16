package com.gymback.modules.pos.services;
import com.gymback.modules.pos.dto.*;
import com.gymback.modules.pos.interfaces.ISaleRepository;
import com.gymback.security.*;
import com.gymback.shared.exceptions.*;
import com.gymback.shared.services.IdempotencyService;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;

public class SaleService {
  private final ISaleRepository repo;
  private final AuthorizationService auth;
  private final IdempotencyService idempotency;
  public SaleService(ISaleRepository repo,AuthorizationService auth){this(repo,auth,null);}
  public SaleService(ISaleRepository repo,AuthorizationService auth,IdempotencyService idempotency){this.repo=repo;this.auth=auth;this.idempotency=idempotency;}
  @Transactional(readOnly=true)
  public List<SaleView> list(UUID branch,int limit) {
    auth.requireModule(branch,"pos","owner","trainer");
    var sales=repo.sales(branch,limit);
    var ids=sales.stream().map(ISaleRepository.SaleSnapshot::id).toList();
    var items=repo.items(branch,ids);
    return sales.stream().map(s->view(s,items)).toList();
  }
  @Transactional
  public SaleView checkout(UUID branch,CheckoutRequest input){return checkout(branch,input,null);}
  @Transactional
  public SaleView checkout(UUID branch,CheckoutRequest input,String idempotencyKey) {
    Actor actor=auth.requireModule(branch,"pos","owner","trainer");
    if(input==null || input.items()==null || input.items().isEmpty() || input.items().size()>100)
      throw new DomainException("Carrito inválido");
    Map<UUID,Integer> quantities=new TreeMap<>();
    for(var item:input.items()) {
      if(item==null || item.productId()==null || item.quantity()<1 || item.quantity()>1000)
        throw new DomainException("Cantidad inválida");
      quantities.merge(item.productId(),item.quantity(),Integer::sum);
      if(quantities.get(item.productId())>1000) throw new DomainException("Cantidad acumulada inválida");
    }
    if(idempotencyKey!=null && idempotency==null)throw new IllegalStateException("Idempotencia no configurada");
    var claim=idempotency==null?null:idempotency.claim(branch,actor.id(),"sale",idempotencyKey,
      new CheckoutRequest(quantities.entrySet().stream()
        .map(e->new CheckoutRequest.Line(e.getKey(),e.getValue())).toList()));
    if(claim!=null && claim.replay()){
      var existing=repo.sale(branch,claim.existingResourceId())
        .orElseThrow(()->new NotFoundException("Venta de solicitud idempotente no encontrada"));
      return view(existing,repo.items(branch,List.of(existing.id())));
    }
    List<Map.Entry<ISaleRepository.ProductSnapshot,Integer>> locked=new ArrayList<>();
    BigDecimal total=BigDecimal.ZERO;
    for(var e:quantities.entrySet()) {
      var product=repo.lockProduct(branch,e.getKey())
        .orElseThrow(()->new DomainException("Producto no existe o está inactivo"));
      if(product.stock()<e.getValue()) throw new DomainException("Stock insuficiente: "+product.name());
      total=total.add(product.price().multiply(BigDecimal.valueOf(e.getValue())));
      locked.add(new AbstractMap.SimpleImmutableEntry<>(product,e.getValue()));
    }
    UUID sale=repo.createSale(branch,total,actor.id());
    for(var row:locked) {
      var product=row.getKey();int qty=row.getValue();
      repo.addSaleItem(branch,sale,product,qty);
      repo.reduceStock(branch,product.id(),qty);
      repo.inventoryMovement(branch,product.id(),-qty,sale,actor.id());
    }
    var persisted=repo.sale(branch,sale).orElseThrow(()->new IllegalStateException("Venta no persistida"));
    if(idempotency!=null)idempotency.complete(claim,sale);
    return view(persisted,repo.items(branch,List.of(sale)));
  }
  private SaleView view(ISaleRepository.SaleSnapshot sale,
                        Map<UUID,List<SaleView.SaleItemView>> items) {
    return new SaleView(sale.id(),sale.branchId(),sale.total(),sale.currency(),sale.status(),
      sale.soldBy(),sale.soldAt(),items.getOrDefault(sale.id(),List.of()));
  }
}
