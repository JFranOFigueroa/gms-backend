package com.gymback.modules.plans.services;
import com.gymback.modules.plans.model.MembershipPlan;
import com.gymback.modules.plans.interfaces.IMembershipPlanRepository;
import com.gymback.modules.plans.dto.MembershipPlanRequest;
import com.gymback.shared.services.*;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.security.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class MembershipPlanService extends BaseService<MembershipPlan> {
  private final AuthorizationService auth;
  public MembershipPlanService(IMembershipPlanRepository repo,AuditService audit,AuthorizationService auth) {super(repo,audit);this.auth=auth;}
  @Transactional(readOnly=true) public List<MembershipPlan> all(UUID gymId,int offset,int limit){ auth.require(gymId,"owner","trainer","member"); return list(gymId,offset,limit); }
  @Transactional(readOnly=true) public MembershipPlan one(UUID gymId,UUID id){ auth.require(gymId,"owner","trainer","member"); return getOr404(id,gymId); }
  @Transactional public MembershipPlan add(UUID gymId,MembershipPlanRequest req){
    Actor actor=auth.require(gymId,"owner");
    if(req.name()==null || req.name().isBlank()) throw new DomainException("name es obligatorio");if(req.price()==null) throw new DomainException("price es obligatorio");
    MembershipPlan e=new MembershipPlan();
    e.setGymId(gymId);
    if(req.name()!=null) e.setName(req.name()); if(req.billingPeriod()!=null) e.setBillingPeriod(req.billingPeriod()); if(req.intervalCount()!=null) e.setIntervalCount(req.intervalCount()); if(req.price()!=null) e.setPrice(req.price()); if(req.description()!=null) e.setDescription(req.description()); if(req.isActive()!=null) e.setIsActive(req.isActive());

    validate(e);
    return create(e,actor.id());
  }
  @Transactional public MembershipPlan change(UUID gymId,UUID id,MembershipPlanRequest req){
    Actor actor=auth.require(gymId,"owner");
    MembershipPlan e=getOr404(id,gymId);
    if(req.name()!=null) e.setName(req.name()); if(req.billingPeriod()!=null) e.setBillingPeriod(req.billingPeriod()); if(req.intervalCount()!=null) e.setIntervalCount(req.intervalCount()); if(req.price()!=null) e.setPrice(req.price()); if(req.description()!=null) e.setDescription(req.description()); if(req.isActive()!=null) e.setIsActive(req.isActive());
    validate(e);
    return update(e,actor.id());
  }
  @Transactional public void remove(UUID gymId,UUID id){
    Actor actor=auth.require(gymId,"owner");
    MembershipPlan e=getOr404(id,gymId);
    e.setIsActive(false);softDelete(e,actor.id());
  }
  private void validate(MembershipPlan e){ if(e.getPrice()==null || e.getPrice().signum()<0)throw new DomainException("Precio inválido");
  if(!Set.of("monthly","yearly").contains(e.getBillingPeriod()))throw new DomainException("Periodo inválido");
  if(e.getIntervalCount()==null || e.getIntervalCount()<1)throw new DomainException("Intervalo inválido"); }
}
