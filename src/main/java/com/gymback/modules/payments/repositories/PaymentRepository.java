package com.gymback.modules.payments.repositories;
import com.gymback.modules.payments.interfaces.IPaymentRepository;
import com.gymback.modules.gyms.model.Gym;
import com.gymback.modules.members.model.GymMember;
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
  @Override public Optional<Plan> plan(UUID gym,UUID id){
    return em.createQuery("select p from MembershipPlan p where p.id=:id and p.gymId=:gym",MembershipPlan.class)
      .setParameter("id",id).setParameter("gym",gym).getResultStream().findFirst()
      .map(p->new Plan(p.getId(),p.getBillingPeriod(),p.getIntervalCount(),p.getPrice(),Boolean.TRUE.equals(p.getIsActive())));
  }
  @Override public Optional<Subscription> lockActiveSubscription(UUID gym,UUID member){
    return em.createQuery("select s from MemberSubscription s where s.gymId=:gym and s.memberId=:member and s.status='active'",MemberSubscription.class)
      .setParameter("gym",gym).setParameter("member",member).setLockMode(LockModeType.PESSIMISTIC_WRITE)
      .getResultStream().findFirst().map(s->new Subscription(s.id,s.planId,s.startedOn,s.nextDueDate));
  }
  @Override public boolean activeMember(UUID gym,UUID member){
    // Bloquea al miembro antes de comprobar/crear su suscripción, incluso si aún no existe.
    // Así dos pagos simultáneos del mismo miembro no intentan crear dos suscripciones activas.
    return !em.createQuery("select m from GymMember m where m.gymId=:gym and m.id=:member and m.isActive=true",GymMember.class)
      .setParameter("gym",gym).setParameter("member",member)
      .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList().isEmpty();
  }
  @Override public String gymTimezone(UUID gym){
    Gym g=em.find(Gym.class,gym);
    if(g==null || !"active".equals(g.getStatus()))throw new com.gymback.shared.exceptions.NotFoundException("Gimnasio no disponible");
    return g.getTimezone();
  }
  @Override public UUID createSubscription(UUID gym,UUID member,UUID plan,LocalDate started,LocalDate due){
    MemberSubscription s=new MemberSubscription();s.gymId=gym;s.memberId=member;s.planId=plan;s.startedOn=started;
    s.nextDueDate=due;s.status="active";em.persist(s);em.flush();return s.id;
  }
  @Override public void updateSubscription(UUID id,LocalDate paid,LocalDate nextDue){
    MemberSubscription s=em.find(MemberSubscription.class,id);
    if(s==null)throw new com.gymback.shared.exceptions.NotFoundException("Membresía no encontrada");
    s.lastPaymentDate=paid;s.nextDueDate=nextDue;em.flush();
  }
  @Override public UUID insertPayment(UUID gym,UUID member,UUID sub,BigDecimal amount,String method,String periodicity,int periods,
                                      LocalDate from,LocalDate due,String notes,UUID actor){
    PaymentEntry p=new PaymentEntry();p.gymId=gym;p.memberId=member;p.subscriptionId=sub;p.amount=amount;
    p.method=method;p.periodicity=periodicity;p.periods=periods;p.periodStart=from;p.nextDueDate=due;
    p.notes=notes;p.registeredBy=actor;em.persist(p);em.flush();return p.id;
  }
  @Override public List<Map<String,Object>> list(UUID gym,UUID member,int limit){
    if(limit<1||limit>100)throw new IllegalArgumentException("limit 1..100");
    var q=em.createQuery("select p from PaymentEntry p where p.gymId=:gym"+
      (member==null?"":" and p.memberId=:member")+" order by p.paidAt desc",PaymentEntry.class)
      .setParameter("gym",gym).setMaxResults(limit);
    if(member!=null)q.setParameter("member",member);
    return q.getResultStream().map(p->{
      Map<String,Object> m=new LinkedHashMap<>();m.put("id",p.id);m.put("gym_id",p.gymId);m.put("member_id",p.memberId);
      m.put("subscription_id",p.subscriptionId);m.put("amount",p.amount);m.put("method",p.method);m.put("concept",p.concept);
      m.put("periodicity",p.periodicity);m.put("periods",p.periods);m.put("paid_at",p.paidAt);m.put("period_start",p.periodStart);
      m.put("next_due_date",p.nextDueDate);m.put("status",p.status);m.put("external_reference",p.externalReference);
      m.put("notes",p.notes);m.put("registered_by",p.registeredBy);m.put("created_at",p.createdAt);
      m.put("created_by",p.createdBy);m.put("updated_at",p.updatedAt);m.put("updated_by",p.updatedBy);
      m.put("deleted_at",p.deletedAt);m.put("deleted_by",p.deletedBy);return m;
    }).toList();
  }
}
