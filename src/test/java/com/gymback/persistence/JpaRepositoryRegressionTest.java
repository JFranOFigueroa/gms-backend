package com.gymback.persistence;
import com.gymback.modules.gyms.repositories.GymRepository;
import com.gymback.modules.settings.repositories.SettingsRepository;
import com.gymback.modules.pos.repositories.SaleRepository;
import com.gymback.modules.routines.repositories.RoutineStructureRepository;
import com.gymback.modules.routines.dto.RoutineDayRequest;
import com.gymback.security.IdentityRepository;
import com.gymback.shared.persistence.model.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JpaRepositoryRegressionTest {
  @Test void slugLookupIncludesArchivedGymsToRespectPhysicalCitextUniqueConstraint(){
    EntityManager em=mock(EntityManager.class);
    @SuppressWarnings("unchecked")TypedQuery<Long> query=mock(TypedQuery.class);
    when(em.createQuery(contains("GymSlugRegistry"),eq(Long.class))).thenReturn(query);
    when(query.setParameter(anyString(),any())).thenReturn(query);
    when(query.getSingleResult()).thenReturn(1L);
    assertTrue(new GymRepository(em).slugExists("gimnasio",null));
    verify(em).createQuery(contains("GymSlugRegistry"),eq(Long.class));
  }
  @Test void settingsDoNotAllowDynamicUntrustedColumns(){
    EntityManager em=mock(EntityManager.class);UUID gym=UUID.randomUUID();
    GymConfiguration current=new GymConfiguration();current.gymId=gym;
    when(em.find(GymConfiguration.class,gym)).thenReturn(current);
    assertThrows(IllegalArgumentException.class,()->new SettingsRepository(em).patch(gym,Map.of("deleted_at","today")));
    verify(em,never()).flush();
  }
  @Test void reassigningPreviouslyDeletedStaffRestoresTheSameCompositeKey(){
    EntityManager em=mock(EntityManager.class);UUID gym=UUID.randomUUID(),user=UUID.randomUUID(),actor=UUID.randomUUID();
    GymStaffRole existing=new GymStaffRole();existing.gymId=gym;existing.userId=user;existing.role="trainer";
    existing.deletedAt=OffsetDateTime.now();existing.isActive=false;
    when(em.find(eq(GymStaffRole.class),any(GymStaffRole.Key.class),eq(LockModeType.PESSIMISTIC_WRITE)))
      .thenReturn(existing);
    new IdentityRepository(em).assignStaff(user,gym,"trainer",actor);
    assertNull(existing.deletedAt);assertTrue(existing.isActive);assertEquals(actor,existing.assignedBy);
    verify(em,never()).persist(any(GymStaffRole.class));
  }
  @Test void inventoryMovementPersistsTypedEntityAndKeepsSaleLink(){
    EntityManager em=mock(EntityManager.class);UUID gym=UUID.randomUUID(),product=UUID.randomUUID(),sale=UUID.randomUUID(),actor=UUID.randomUUID();
    new SaleRepository(em).inventoryMovement(gym,product,-2,sale,actor);
    var captor=org.mockito.ArgumentCaptor.forClass(InventoryMovement.class);
    verify(em).persist(captor.capture());
    InventoryMovement row=captor.getValue();
    assertEquals(gym,row.gymId);assertEquals(product,row.productId);assertEquals(sale,row.saleId);
    assertEquals(-2,row.stockDelta);assertEquals("sale",row.reason);assertEquals(actor,row.performedBy);
  }
  @Test void invalidRoutineIsRejectedBeforeArchivingExistingDays(){
    EntityManager em=mock(EntityManager.class);
    RoutineStructureRepository repo=new RoutineStructureRepository(em);
    assertThrows(IllegalArgumentException.class,()->repo.replace(UUID.randomUUID(),UUID.randomUUID(),
      List.of(new RoutineDayRequest("",List.of()))));
    verifyNoInteractions(em);
  }
}
