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
   UUID branch=UUID.randomUUID(),actor=UUID.randomUUID(),product=UUID.randomUUID(),sale=UUID.randomUUID();
   when(auth.requireModule(eq(branch),eq("pos"),any(String[].class))).thenReturn(new Actor(actor,false));
   when(idem.claim(eq(branch),eq(actor),eq("sale"),eq("sale-key-123"),any()))
      .thenReturn(new IdempotencyService.Claim(branch,actor,"sale","sale-key-123",sale));
   when(repo.sale(branch,sale)).thenReturn(Optional.of(new ISaleRepository.SaleSnapshot(sale,branch,
     new BigDecimal("50.00"),"MXN","completed",actor,java.time.OffsetDateTime.now())));
   when(repo.items(eq(branch),any())).thenReturn(Map.of());
   var response=new SaleService(repo,auth,idem).checkout(branch,
     new CheckoutRequest(List.of(new CheckoutRequest.Line(product,1))),"sale-key-123");
   assertEquals(sale,response.id());
   verify(repo,never()).reduceStock(any(),any(),anyInt());
   verify(repo,never()).createSale(any(),any(),any());
 }
 @Test void preventsOverselling(){
   var repo=mock(ISaleRepository.class);var auth=mock(AuthorizationService.class);UUID branch=UUID.randomUUID(),prod=UUID.randomUUID();
   when(auth.requireModule(eq(branch),eq("pos"),any(String[].class))).thenReturn(new Actor(UUID.randomUUID(),false));
   when(repo.lockProduct(branch,prod)).thenReturn(Optional.of(new ISaleRepository.ProductSnapshot(prod,"Agua",new BigDecimal("20.00"),1)));
   var service=new SaleService(repo,auth);
   assertThrows(DomainException.class,()->service.checkout(branch,new CheckoutRequest(List.of(new CheckoutRequest.Line(prod,2)))));
   verify(repo,never()).createSale(any(),any(),any());
 }
}
