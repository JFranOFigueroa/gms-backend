package com.gymback.pos;
import com.gymback.modules.pos.dto.SaleView.SaleItemView;
import com.gymback.modules.pos.interfaces.ISaleRepository;
import com.gymback.modules.pos.services.SaleService;
import com.gymback.security.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

class SaleDetailsTest {
  @Test void listsSalesIncludingLinesWithOneBatchQuery() {
    var repo=mock(ISaleRepository.class);var auth=mock(AuthorizationService.class);
    UUID gym=UUID.randomUUID(),saleId=UUID.randomUUID(),product=UUID.randomUUID();
    var sale=new ISaleRepository.SaleSnapshot(saleId,gym,new BigDecimal("80.00"),"MXN","completed",
      UUID.randomUUID(),OffsetDateTime.now());
    var line=new SaleItemView(UUID.randomUUID(),product,"Agua",2,new BigDecimal("40.00"),new BigDecimal("80.00"));
    when(repo.sales(gym,50)).thenReturn(List.of(sale));
    when(repo.items(eq(gym),eq(List.of(saleId)))).thenReturn(Map.of(saleId,List.of(line)));
    var result=new SaleService(repo,auth).list(gym,50);
    assertEquals(1,result.size());
    assertEquals(2,result.get(0).items().get(0).quantity());
    assertEquals("Agua",result.get(0).items().get(0).productName());
    verify(repo,times(1)).items(eq(gym),anyCollection());
  }
}
