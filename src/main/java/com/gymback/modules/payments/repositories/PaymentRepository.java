package com.gymback.modules.payments.repositories;

import com.gymback.modules.payments.interfaces.IPaymentRepository;
import com.gymback.modules.branches.model.Branch;
import com.gymback.modules.members.model.BranchMember;
import com.gymback.modules.plans.model.MembershipPlan;
import com.gymback.shared.persistence.model.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Repository
public class PaymentRepository implements IPaymentRepository {
  private final EntityManager em;
  public PaymentRepository(EntityManager em){this.em=em;}

  @Override public Optional<Plan> plan(UUID branch,UUID id){
    return em.createQuery("select p from MembershipPlan p where p.id=:id and p.branchId=:branch",MembershipPlan.class)
      .setParameter("id",id).setParameter("branch",branch).getResultStream().findFirst()
      .map(p->new Plan(p.getId(),p.getBillingPeriod(),p.getIntervalCount(),p.getPrice(),Boolean.TRUE.equals(p.getIsActive())));
  }

  @Override public Optional<AdminPlan> adminPlan(UUID branch,UUID id){
    // SQLRestriction on AdminPlan excludes soft-deleted plans; lock against simultaneous deactivation.
    return em.createQuery("select p from AdminPlan p where p.id=:id and p.branchId=:branch",
        com.gymback.modules.adminplans.model.AdminPlan.class)
      .setParameter("id",id).setParameter("branch",branch).setLockMode(LockModeType.PESSIMISTIC_READ)
      .getResultStream().findFirst().map(p->new AdminPlan(p.getId(),p.getDurationDays(),p.getPrice(),
        Boolean.TRUE.equals(p.getIsActive())));
  }

  @Override public Optional<Subscription> lockActiveSubscription(UUID branch,UUID member){
    return em.createQuery("select s from MemberSubscription s where s.branchId=:branch and s.memberId=:member and s.status='active'",MemberSubscription.class)
      .setParameter("branch",branch).setParameter("member",member).setLockMode(LockModeType.PESSIMISTIC_WRITE)
      .getResultStream().findFirst()
      .map(s->new Subscription(s.id,s.planId,s.adminPlanId,s.startedOn,s.nextDueDate));
  }

  @Override public boolean activeMember(UUID branch,UUID member){
    // Lock the member even if this is the first subscription: serializes payments for that member.
    return !em.createQuery("select m from BranchMember m where m.branchId=:branch and m.id=:member and m.isActive=true",BranchMember.class)
      .setParameter("branch",branch).setParameter("member",member)
      .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList().isEmpty();
  }

  @Override public String gymTimezone(UUID branch){
    Branch g=em.find(Branch.class,branch);
    if(g==null || !"active".equals(g.getStatus()))
      throw new com.gymback.shared.exceptions.NotFoundException("Sucursal no disponible");
    return g.getTimezone();
  }

  @Override public UUID createSubscription(UUID branch,UUID member,UUID plan,LocalDate started,LocalDate due){
    MemberSubscription s=new MemberSubscription();s.branchId=branch;s.memberId=member;s.planId=plan;
    s.adminPlanId=null;s.startedOn=started;s.nextDueDate=due;s.status="active";
    em.persist(s);em.flush();return s.id;
  }

  @Override public UUID createAdminSubscription(UUID branch,UUID member,UUID adminPlan,LocalDate started,LocalDate due){
    MemberSubscription s=new MemberSubscription();s.branchId=branch;s.memberId=member;s.planId=null;
    s.adminPlanId=adminPlan;s.startedOn=started;s.nextDueDate=due;s.status="active";
    em.persist(s);em.flush();return s.id;
  }

  @Override public void updateSubscription(UUID id,LocalDate paid,LocalDate nextDue){
    MemberSubscription s=em.find(MemberSubscription.class,id);
    if(s==null)throw new com.gymback.shared.exceptions.NotFoundException("Membresía no encontrada");
    s.lastPaymentDate=paid;s.nextDueDate=nextDue;em.flush();
  }

  @Override public UUID insertPayment(UUID branch,UUID member,UUID sub,BigDecimal amount,String method,String periodicity,
                                      int periods,LocalDate from,LocalDate due,String notes,UUID actor){
    MemberSubscription subscription=em.find(MemberSubscription.class,sub);
    if(subscription==null || subscription.planId==null)
      throw new IllegalStateException("La suscripción no corresponde al catálogo de membresías");
    return savePayment(branch,member,sub,subscription.planId,null,amount,method,periodicity,periods,from,due,notes,actor);
  }

  @Override public UUID insertAdminPayment(UUID branch,UUID member,UUID sub,UUID adminPlan,BigDecimal amount,String method,
                                           int periods,LocalDate from,LocalDate due,String notes,UUID actor){
    return savePayment(branch,member,sub,null,adminPlan,amount,method,"daily",periods,from,due,notes,actor);
  }

  private UUID savePayment(UUID branch,UUID member,UUID sub,UUID planId,UUID adminPlanId,BigDecimal amount,
                           String method,String periodicity,int periods,LocalDate from,LocalDate due,String notes,UUID actor){
    PaymentEntry p=new PaymentEntry();p.branchId=branch;p.memberId=member;p.subscriptionId=sub;
    p.planId=planId;p.adminPlanId=adminPlanId;p.amount=amount;p.method=method;p.periodicity=periodicity;
    p.periods=periods;p.periodStart=from;p.nextDueDate=due;p.notes=notes;p.registeredBy=actor;
    em.persist(p);em.flush();return p.id;
  }

  @Override public Optional<Map<String,Object>> receipt(UUID branch,UUID id){
    return em.createQuery("select p from PaymentEntry p where p.branchId=:branch and p.id=:id",PaymentEntry.class)
      .setParameter("branch",branch).setParameter("id",id).setMaxResults(1).getResultStream().findFirst()
      .map(p->{
        Map<String,Object> v=new LinkedHashMap<>();
        v.put("id",p.id);v.put("memberId",p.memberId);v.put("subscriptionId",p.subscriptionId);
        v.put("amount",p.amount);v.put("periodStart",p.periodStart);v.put("nextDueDate",p.nextDueDate);
        v.put("periods",p.periods);v.put("planId",p.planId);v.put("adminPlanId",p.adminPlanId);
        v.put("planSource",p.adminPlanId!=null?"admin":"membership");
        return v;
      });
  }

  @Override public List<Map<String,Object>> list(UUID branch,UUID member,int limit){
    if(limit<1||limit>100)throw new IllegalArgumentException("limit 1..100");
    var q=em.createQuery("select p from PaymentEntry p where p.branchId=:branch"+
      (member==null?"":" and p.memberId=:member")+" order by p.paidAt desc",PaymentEntry.class)
      .setParameter("branch",branch).setMaxResults(limit);
    if(member!=null)q.setParameter("member",member);
    return q.getResultStream().map(p->{
      Map<String,Object> m=new LinkedHashMap<>();
      m.put("id",p.id);m.put("branch_id",p.branchId);m.put("member_id",p.memberId);
      m.put("subscription_id",p.subscriptionId);m.put("amount",p.amount);m.put("method",p.method);m.put("concept",p.concept);
      m.put("periodicity",p.periodicity);m.put("periods",p.periods);m.put("paid_at",p.paidAt);m.put("period_start",p.periodStart);
      m.put("next_due_date",p.nextDueDate);m.put("status",p.status);m.put("external_reference",p.externalReference);
      m.put("notes",p.notes);m.put("registered_by",p.registeredBy);m.put("created_at",p.createdAt);
      m.put("created_by",p.createdBy);m.put("updated_at",p.updatedAt);m.put("updated_by",p.updatedBy);
      m.put("deleted_at",p.deletedAt);m.put("deleted_by",p.deletedBy);
      m.put("plan_id",p.planId);m.put("admin_plan_id",p.adminPlanId);
      m.put("plan_source",p.adminPlanId!=null?"admin":"membership");
      return m;
    }).toList();
  }
}
