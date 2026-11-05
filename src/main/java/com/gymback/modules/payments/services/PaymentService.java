package com.gymback.modules.payments.services;
import com.gymback.modules.payments.dto.PaymentRequest;
import com.gymback.modules.payments.interfaces.IPaymentRepository;
import com.gymback.security.*;
import com.gymback.shared.exceptions.DomainException;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
public class PaymentService {
  private final IPaymentRepository repo;private final AuthorizationService auth;
  public PaymentService(IPaymentRepository repo,AuthorizationService auth){this.repo=repo;this.auth=auth;}
  @Transactional(readOnly=true) public List<Map<String,Object>> list(UUID gym,UUID member,int limit){auth.require(gym,"owner");return repo.list(gym,member,limit);}
  @Transactional public Map<String,Object> register(UUID gym,PaymentRequest d){
    Actor actor=auth.require(gym,"owner");
    if(d.memberId()==null||d.planId()==null||d.method()==null)throw new DomainException("Miembro, plan y método obligatorios");
    if(!Set.of("cash","card","transfer","other").contains(d.method()))throw new DomainException("Método inválido");
    int periods=d.periods()==null?1:d.periods();if(periods<1||periods>12)throw new DomainException("periods debe estar entre 1 y 12");
    if(!repo.activeMember(gym,d.memberId()))throw new DomainException("Miembro no activo en gimnasio");
    var plan=repo.plan(gym,d.planId()).orElseThrow(()->new DomainException("Plan inexistente"));
    if(!plan.active())throw new DomainException("Plan inactivo");
    var subscription=repo.lockActiveSubscription(gym,d.memberId());
    if(subscription.isPresent() && !subscription.get().planId().equals(plan.id()))throw new DomainException("El miembro ya tiene un plan activo distinto");
    LocalDate today=LocalDate.now(ZoneId.of(repo.gymTimezone(gym)));
    LocalDate from=subscription.map(s->s.nextDue().isAfter(today)?s.nextDue():today).orElse(today);
    LocalDate due=plan.period().equals("yearly")?from.plusYears((long)plan.intervalCount()*periods):from.plusMonths((long)plan.intervalCount()*periods);
    UUID sub=subscription.isPresent()?subscription.get().id():repo.createSubscription(gym,d.memberId(),plan.id(),today,due);
    repo.updateSubscription(sub,today,due);
    var amount=plan.price().multiply(java.math.BigDecimal.valueOf(periods));
    UUID id=repo.insertPayment(gym,d.memberId(),sub,amount,d.method(),plan.period(),periods,from,due,d.notes(),actor.id());
    return Map.of("id",id,"memberId",d.memberId(),"subscriptionId",sub,"amount",amount,"periodStart",from,"nextDueDate",due,"periods",periods);
  }
}
