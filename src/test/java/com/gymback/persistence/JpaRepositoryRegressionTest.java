package com.gymback.persistence;
import com.gymback.modules.branches.repositories.BranchRepository;
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
  @Test void codeLookupIncludesArchivedGymsToRespectPhysicalCitextUniqueConstraint(){
    EntityManager em=mock(EntityManager.class);
    @SuppressWarnings("unchecked")TypedQuery<Long> query=mock(TypedQuery.class);
    when(em.createQuery(contains("BranchCodeRegistry"),eq(Long.class))).thenReturn(query);
    when(query.setParameter(anyString(),any())).thenReturn(query);
    when(query.getSingleResult()).thenReturn(1L);
    assertTrue(new BranchRepository(em).codeExists("sucursal",null));
    verify(em).createQuery(contains("BranchCodeRegistry"),eq(Long.class));
  }
  @Test void settingsDoNotAllowDynamicUntrustedColumns(){
    EntityManager em=mock(EntityManager.class);UUID branch=UUID.randomUUID();
    BranchSettings current=new BranchSettings();current.branchId=branch;
    when(em.find(BranchSettings.class,branch)).thenReturn(current);
    assertThrows(IllegalArgumentException.class,()->new SettingsRepository(em).patch(branch,Map.of("deleted_at","today")));
    verify(em,never()).flush();
  }
  @Test void reassigningPreviouslyDeletedStaffRestoresTheSameCompositeKey(){
    EntityManager em=mock(EntityManager.class);UUID branch=UUID.randomUUID(),user=UUID.randomUUID(),actor=UUID.randomUUID();
    BranchStaffRole existing=new BranchStaffRole();existing.branchId=branch;existing.userId=user;existing.role="trainer";
    existing.deletedAt=OffsetDateTime.now();existing.isActive=false;
    when(em.find(eq(BranchStaffRole.class),any(BranchStaffRole.Key.class),eq(LockModeType.PESSIMISTIC_WRITE)))
      .thenReturn(existing);
    new IdentityRepository(em).assignStaff(user,branch,"trainer",actor);
    assertNull(existing.deletedAt);assertTrue(existing.isActive);assertEquals(actor,existing.assignedBy);
    verify(em,never()).persist(any(BranchStaffRole.class));
  }
  @Test void inventoryMovementPersistsTypedEntityAndKeepsSaleLink(){
    EntityManager em=mock(EntityManager.class);UUID branch=UUID.randomUUID(),product=UUID.randomUUID(),sale=UUID.randomUUID(),actor=UUID.randomUUID();
    new SaleRepository(em).inventoryMovement(branch,product,-2,sale,actor);
    var captor=org.mockito.ArgumentCaptor.forClass(InventoryMovement.class);
    verify(em).persist(captor.capture());
    InventoryMovement row=captor.getValue();
    assertEquals(branch,row.branchId);assertEquals(product,row.productId);assertEquals(sale,row.saleId);
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
