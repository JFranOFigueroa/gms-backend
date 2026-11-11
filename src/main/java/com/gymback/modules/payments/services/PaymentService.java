package com.gymback.modules.payments.services;
import com.gymback.modules.payments.dto.PaymentRequest;
import com.gymback.modules.payments.interfaces.IPaymentRepository;
import com.gymback.security.*;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.exceptions.NotFoundException;
import com.gymback.shared.services.IdempotencyService;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
public class PaymentService {
  private final IPaymentRepository repo;private final AuthorizationService auth;private final IdempotencyService idempotency;
  public PaymentService(IPaymentRepository repo,AuthorizationService auth){this(repo,auth,null);}
  public PaymentService(IPaymentRepository repo,AuthorizationService auth,IdempotencyService idempotency){this.repo=repo;this.auth=auth;this.idempotency=idempotency;}
  @Transactional(readOnly=true) public List<Map<String,Object>> list(UUID gym,UUID member,int limit){auth.require(gym,"owner");return repo.list(gym,member,limit);}
  @Transactional public Map<String,Object> register(UUID gym,PaymentRequest d){return register(gym,d,null);}
  @Transactional public Map<String,Object> register(UUID gym,PaymentRequest d,String idempotencyKey){
    Actor actor=auth.require(gym,"owner");
    if(d==null)throw new DomainException("Pago obligatorio");
    if(d.memberId()==null||d.planId()==null||d.method()==null)throw new DomainException("Miembro, plan y método obligatorios");
    if(!Set.of("cash","card","transfer","other").contains(d.method()))throw new DomainException("Método inválido");
    int periods=d.periods()==null?1:d.periods();if(periods<1||periods>12)throw new DomainException("periods debe estar entre 1 y 12");
    if(idempotencyKey!=null && idempotency==null)throw new IllegalStateException("Idempotencia no configurada");
    // Acquire the idempotency row before the member/subscription locks.
    var claim=idempotency==null?null:idempotency.claim(gym,actor.id(),"payment",idempotencyKey,
      new PaymentRequest(d.memberId(),d.planId(),d.method(),periods,d.notes()));
    if(claim!=null && claim.replay())return repo.receipt(gym,claim.existingResourceId())
      .orElseThrow(()->new NotFoundException("Pago de solicitud idempotente no encontrado"));
    if(!repo.activeMember(gym,d.memberId()))throw new DomainException("Miembro no activo en gimnasio");
    var plan=repo.plan(gym,d.planId()).orElseThrow(()->new DomainException("Plan inexistente"));
    if(!plan.active())throw new DomainException("Plan inactivo");
    if(plan.price()==null || plan.price().signum()<=0)throw new DomainException("Un plan gratuito es informativo y no admite registro de pago");
    var subscription=repo.lockActiveSubscription(gym,d.memberId());
    if(subscription.isPresent() && !subscription.get().planId().equals(plan.id()))throw new DomainException("El miembro ya tiene un plan activo distinto");
    LocalDate today=LocalDate.now(ZoneId.of(repo.gymTimezone(gym)));
    LocalDate from=subscription.map(s->s.nextDue().isAfter(today)?s.nextDue():today).orElse(today);
    LocalDate due=plan.period().equals("yearly")?from.plusYears((long)plan.intervalCount()*periods):from.plusMonths((long)plan.intervalCount()*periods);
    UUID sub=subscription.isPresent()?subscription.get().id():repo.createSubscription(gym,d.memberId(),plan.id(),today,due);
    repo.updateSubscription(sub,today,due);
    var amount=plan.price().multiply(java.math.BigDecimal.valueOf(periods));
    UUID id=repo.insertPayment(gym,d.memberId(),sub,amount,d.method(),plan.period(),periods,from,due,d.notes(),actor.id());
    if(idempotency!=null)idempotency.complete(claim,id);
    return Map.of("id",id,"memberId",d.memberId(),"subscriptionId",sub,"amount",amount,"periodStart",from,"nextDueDate",due,"periods",periods);
  }
}
