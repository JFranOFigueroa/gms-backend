package com.gymback.pos;
import com.gymback.modules.pos.services.SaleService;
import com.gymback.modules.pos.interfaces.ISaleRepository;
import com.gymback.modules.pos.dto.CheckoutRequest;
import com.gymback.security.*;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.services.IdempotencyService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.math.*;
class SaleServiceTest {
 @Test void repeatingSameSaleKeyKeepsExistingSaleAndStock() {
   var repo=mock(ISaleRepository.class);var auth=mock(AuthorizationService.class);
   var idem=mock(IdempotencyService.class);
   UUID gym=UUID.randomUUID(),actor=UUID.randomUUID(),product=UUID.randomUUID(),sale=UUID.randomUUID();
   when(auth.requireModule(eq(gym),eq("pos"),any(String[].class))).thenReturn(new Actor(actor,false));
   when(idem.claim(eq(gym),eq(actor),eq("sale"),eq("sale-key-123"),any()))
      .thenReturn(new IdempotencyService.Claim(gym,actor,"sale","sale-key-123",sale));
   when(repo.sale(gym,sale)).thenReturn(Optional.of(new ISaleRepository.SaleSnapshot(sale,gym,
     new BigDecimal("50.00"),"MXN","completed",actor,java.time.OffsetDateTime.now())));
   when(repo.items(eq(gym),any())).thenReturn(Map.of());
   var response=new SaleService(repo,auth,idem).checkout(gym,
     new CheckoutRequest(List.of(new CheckoutRequest.Line(product,1))),"sale-key-123");
   assertEquals(sale,response.id());
   verify(repo,never()).reduceStock(any(),any(),anyInt());
   verify(repo,never()).createSale(any(),any(),any());
 }
 @Test void preventsOverselling(){
   var repo=mock(ISaleRepository.class);var auth=mock(AuthorizationService.class);UUID gym=UUID.randomUUID(),prod=UUID.randomUUID();
   when(auth.requireModule(eq(gym),eq("pos"),any(String[].class))).thenReturn(new Actor(UUID.randomUUID(),false));
   when(repo.lockProduct(gym,prod)).thenReturn(Optional.of(new ISaleRepository.ProductSnapshot(prod,"Agua",new BigDecimal("20.00"),1)));
   var service=new SaleService(repo,auth);
   assertThrows(DomainException.class,()->service.checkout(gym,new CheckoutRequest(List.of(new CheckoutRequest.Line(prod,2)))));
   verify(repo,never()).createSale(any(),any(),any());
 }
}
