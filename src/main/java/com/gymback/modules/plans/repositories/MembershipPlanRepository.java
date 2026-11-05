package com.gymback.modules.plans.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.plans.model.MembershipPlan;
import com.gymback.modules.plans.interfaces.IMembershipPlanRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
@Repository
public class MembershipPlanRepository extends BaseCRUD<MembershipPlan> implements IMembershipPlanRepository {
  public MembershipPlanRepository(EntityManager em){super(em,MembershipPlan.class,true);}
}
