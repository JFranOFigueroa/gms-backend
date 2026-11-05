package com.gymback.shared;
import com.gymback.modules.pos.model.Product;
import com.gymback.modules.pos.repositories.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class BaseCrudTenantTest {
 @Test void lookupDoesNotLeakAcrossGyms(){
   EntityManager em=mock(EntityManager.class);
   Product p=new Product();UUID gymA=UUID.randomUUID();p.setGymId(gymA);
   when(em.find(Product.class,p.getId())).thenReturn(p);
   var repo=new ProductRepository(em);
   assertTrue(repo.get(p.getId(),UUID.randomUUID()).isEmpty());
   assertTrue(repo.get(p.getId(),gymA).isPresent());
   assertThrows(IllegalArgumentException.class,()->repo.get(p.getId(),null));
 }
}
