package com.gymback.modules.adminplans.repositories;

import com.gymback.modules.adminplans.interfaces.IAdminPlanRepository;
import com.gymback.modules.adminplans.model.AdminPlan;
import com.gymback.shared.repositories.BaseCRUD;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

@Repository
public class AdminPlanRepository extends BaseCRUD<AdminPlan> implements IAdminPlanRepository {
  public AdminPlanRepository(EntityManager em) {
    super(em, AdminPlan.class, true);
  }
}
