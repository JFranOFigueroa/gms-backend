package com.gymback.pos;
import com.gymback.modules.pos.services.SaleService;
import com.gymback.modules.pos.interfaces.ISaleRepository;
import com.gymback.modules.pos.dto.CheckoutRequest;
import com.gymback.security.*;
import com.gymback.shared.exceptions.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.math.*;
class SaleServiceTest {
 @Test void preventsOverselling(){
   var repo=mock(ISaleRepository.class);var auth=mock(AuthorizationService.class);UUID gym=UUID.randomUUID(),prod=UUID.randomUUID();
   when(auth.requireModule(eq(gym),eq("pos"),any(String[].class))).thenReturn(new Actor(UUID.randomUUID(),false));
   when(repo.lockProduct(gym,prod)).thenReturn(Optional.of(new ISaleRepository.ProductSnapshot(prod,"Agua",new BigDecimal("20.00"),1)));
   var service=new SaleService(repo,auth);
   assertThrows(DomainException.class,()->service.checkout(gym,new CheckoutRequest(List.of(new CheckoutRequest.Line(prod,2)))));
   verify(repo,never()).createSale(any(),any(),any());
 }
}
