package com.gymback.adminplans;

import com.gymback.modules.adminplans.dto.AdminPlanRequest;
import com.gymback.modules.adminplans.dto.AdminPlanResponse;
import com.gymback.modules.adminplans.interfaces.IAdminPlanRepository;
import com.gymback.modules.adminplans.model.AdminPlan;
import com.gymback.modules.adminplans.services.AdminPlanService;
import com.gymback.security.Actor;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.exceptions.NotFoundException;
import com.gymback.shared.services.AuditService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminPlanServiceTest {
  private IAdminPlanRepository repo;
  private AuthorizationService auth;
  private AdminPlanService service;
  private final UUID branchId = UUID.randomUUID();
  private final UUID ownerId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    repo = mock(IAdminPlanRepository.class);
    auth = mock(AuthorizationService.class);
    when(auth.require(branchId, "owner")).thenReturn(new Actor(ownerId, false));
    when(repo.create(any(AdminPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(repo.update(any(AdminPlan.class))).thenAnswer(invocation -> invocation.getArgument(0));
    service = new AdminPlanService(repo, new AuditService(), auth);
  }

  @Test
  void createAssignsBranchAndAllInitialAuditMetadata() {
    AdminPlan plan = service.add(branchId,
        new AdminPlanRequest(" Plan mensual ", " Acceso a instalaciones ",
            new BigDecimal("400.00"), 30, null));
    assertEquals(branchId, plan.getBranchId());
    assertEquals("Plan mensual", plan.getName());
    assertEquals("Acceso a instalaciones", plan.getDescription());
    assertEquals(new BigDecimal("400.00"), plan.getPrice());
    assertEquals(30, plan.getDurationDays());
    assertTrue(plan.getIsActive());
    assertEquals(ownerId, plan.getCreatedBy());
    assertEquals(ownerId, plan.getUpdatedBy());
    assertNotNull(plan.getCreatedAt());
    assertNotNull(plan.getUpdatedAt());
    assertNull(plan.getDeletedAt());
    assertNull(plan.getDeletedBy());
    verify(repo).create(plan);
  }

  @Test
  void patchOnlyChangesSuppliedFieldsAndKeepsCreatedAudit() {
    AdminPlan existing = sample();
    UUID creator = UUID.randomUUID();
    existing.setCreatedBy(creator);
    existing.setDescription("Anterior");
    when(repo.get(existing.getId(), branchId)).thenReturn(Optional.of(existing));
    AdminPlan saved = service.change(branchId, existing.getId(),
        new AdminPlanRequest(null, "", new BigDecimal("500"), null, false));
    assertEquals("Original", saved.getName());
    assertNull(saved.getDescription());
    assertEquals(new BigDecimal("500"), saved.getPrice());
    assertFalse(saved.getIsActive());
    assertEquals(creator, saved.getCreatedBy());
    assertEquals(ownerId, saved.getUpdatedBy());
    verify(repo).update(existing);
  }

  @Test
  void deleteIsSoftAndPreservesAudit() {
    AdminPlan existing = sample();
    UUID creator = UUID.randomUUID();
    existing.setCreatedBy(creator);
    when(repo.get(existing.getId(), branchId)).thenReturn(Optional.of(existing));
    service.remove(branchId, existing.getId());
    assertFalse(existing.getIsActive());
    assertNotNull(existing.getDeletedAt());
    assertEquals(ownerId, existing.getDeletedBy());
    assertEquals(ownerId, existing.getUpdatedBy());
    assertEquals(creator, existing.getCreatedBy());
    verify(repo).update(existing);
    verify(repo, never()).delete(any());
  }

  @Test
  void doesNotReadOtherBranch() {
    UUID otherBranch = UUID.randomUUID();
    when(auth.require(otherBranch, "owner")).thenReturn(new Actor(ownerId, false));
    assertThrows(NotFoundException.class, () -> service.one(otherBranch, UUID.randomUUID()));
    verify(repo).get(any(UUID.class), eq(otherBranch));
  }

  @Test
  void trainerCannotReadOrWriteAdminPlans() {
    when(auth.require(branchId, "owner")).thenThrow(new AccessDeniedException("denied"));
    assertThrows(AccessDeniedException.class, () -> service.all(branchId, 0, 20));
    assertThrows(AccessDeniedException.class, () -> service.add(branchId,
        new AdminPlanRequest("Plan", null, new BigDecimal("100"), 30, true)));
    verifyNoInteractions(repo);
  }

  @Test
  void rejectsInvalidPriceNameAndDurationBeforeWriting() {
    assertThrows(DomainException.class, () -> service.add(branchId,
        new AdminPlanRequest("", null, new BigDecimal("100"), 30, true)));
    assertThrows(DomainException.class, () -> service.add(branchId,
        new AdminPlanRequest("Plan", null, new BigDecimal("10.999"), 30, true)));
    assertThrows(DomainException.class, () -> service.add(branchId,
        new AdminPlanRequest("Plan", null, new BigDecimal("-1"), 30, true)));
    assertThrows(DomainException.class, () -> service.add(branchId,
        new AdminPlanRequest("Plan", null, new BigDecimal("100"), 0, true)));
    verify(repo, never()).create(any());
  }

  @Test
  void responseContractOmitsAuditFields() {
    AdminPlanResponse response = AdminPlanResponse.from(sample());
    assertEquals(branchId, response.branchId());
    assertEquals(7, AdminPlanResponse.class.getRecordComponents().length);
  }

  private AdminPlan sample() {
    AdminPlan plan = new AdminPlan();
    plan.setBranchId(branchId);
    plan.setName("Original");
    plan.setPrice(new BigDecimal("100"));
    plan.setDurationDays(30);
    return plan;
  }
}
