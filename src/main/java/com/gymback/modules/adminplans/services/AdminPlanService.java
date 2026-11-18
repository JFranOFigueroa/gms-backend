package com.gymback.modules.adminplans.services;

import com.gymback.modules.adminplans.dto.AdminPlanRequest;
import com.gymback.modules.adminplans.interfaces.IAdminPlanRepository;
import com.gymback.modules.adminplans.model.AdminPlan;
import com.gymback.security.Actor;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.services.AuditService;
import com.gymback.shared.services.BaseService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Catálogo interno por gimnasio; no crea suscripciones ni cobra membresías. */
public class AdminPlanService extends BaseService<AdminPlan> {
  private static final BigDecimal MAX_PRICE = new BigDecimal("9999999999.99");
  private final AuthorizationService auth;

  public AdminPlanService(IAdminPlanRepository repo, AuditService audit, AuthorizationService auth) {
    super(repo, audit);
    this.auth = auth;
  }

  @Transactional(readOnly = true)
  public List<AdminPlan> all(UUID branchId, int offset, int limit) {
    auth.require(branchId, "owner");
    return list(branchId, offset, limit);
  }

  @Transactional(readOnly = true)
  public AdminPlan one(UUID branchId, UUID id) {
    auth.require(branchId, "owner");
    return getOr404(id, branchId);
  }

  @Transactional
  public AdminPlan add(UUID branchId, AdminPlanRequest request) {
    Actor actor = auth.require(branchId, "owner");
    if (request == null || request.name() == null || request.price() == null || request.durationDays() == null) {
      throw new DomainException("name, price y durationDays son obligatorios");
    }
    AdminPlan plan = new AdminPlan();
    plan.setBranchId(branchId);
    apply(plan, request);
    validate(plan);
    return create(plan, actor.id());
  }

  @Transactional
  public AdminPlan change(UUID branchId, UUID id, AdminPlanRequest request) {
    Actor actor = auth.require(branchId, "owner");
    if (request == null) throw new DomainException("Datos del plan obligatorios");
    AdminPlan plan = getOr404(id, branchId);
    apply(plan, request);
    validate(plan);
    return update(plan, actor.id());
  }

  @Transactional
  public void remove(UUID branchId, UUID id) {
    Actor actor = auth.require(branchId, "owner");
    AdminPlan plan = getOr404(id, branchId);
    plan.setIsActive(false);
    softDelete(plan, actor.id());
  }

  private void apply(AdminPlan plan, AdminPlanRequest request) {
    if (request.name() != null) plan.setName(request.name().trim());
    // description: null = sin cambio en PATCH; "" = limpiar descripción.
    if (request.description() != null) {
      String description = request.description().trim();
      plan.setDescription(description.isEmpty() ? null : description);
    }
    if (request.price() != null) plan.setPrice(request.price());
    if (request.durationDays() != null) plan.setDurationDays(request.durationDays());
    if (request.isActive() != null) plan.setIsActive(request.isActive());
  }

  private void validate(AdminPlan plan) {
    if (plan.getName() == null || plan.getName().isBlank() || plan.getName().length() > 120) {
      throw new DomainException("name debe contener entre 1 y 120 caracteres");
    }
    if (plan.getDescription() != null && plan.getDescription().length() > 1000) {
      throw new DomainException("description no puede superar 1000 caracteres");
    }
    if (plan.getPrice() == null || plan.getPrice().signum() < 0
        || plan.getPrice().compareTo(MAX_PRICE) > 0 || plan.getPrice().stripTrailingZeros().scale() > 2) {
      throw new DomainException("price debe ser mayor o igual a 0 y tener máximo 2 decimales");
    }
    if (plan.getDurationDays() == null || plan.getDurationDays() < 1 || plan.getDurationDays() > 3650) {
      throw new DomainException("durationDays debe estar entre 1 y 3650");
    }
  }
}
