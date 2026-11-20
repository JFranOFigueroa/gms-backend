package com.gymback.modules.payments.services;

import com.gymback.modules.payments.dto.PaymentRequest;
import com.gymback.modules.payments.interfaces.IPaymentRepository;
import com.gymback.security.*;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.exceptions.NotFoundException;
import com.gymback.shared.services.IdempotencyService;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public class PaymentService {
  private static final BigDecimal MAX_STORED_AMOUNT = new BigDecimal("9999999999.99");
  private final IPaymentRepository repo;
  private final AuthorizationService auth;
  private final IdempotencyService idempotency;

  public PaymentService(IPaymentRepository repo,AuthorizationService auth){this(repo,auth,null);}
  public PaymentService(IPaymentRepository repo,AuthorizationService auth,IdempotencyService idempotency){
    this.repo=repo;this.auth=auth;this.idempotency=idempotency;
  }

  @Transactional(readOnly=true)
  public List<Map<String,Object>> list(UUID branch,UUID member,int limit){
    auth.require(branch,"owner");return repo.list(branch,member,limit);
  }

  @Transactional
  public Map<String,Object> register(UUID branch,PaymentRequest request){return register(branch,request,null);}

  @Transactional
  public Map<String,Object> register(UUID branch,PaymentRequest d,String idempotencyKey){
    Actor actor=auth.require(branch,"owner");
    if(d==null)throw new DomainException("Pago obligatorio");
    if(d.memberId()==null || d.method()==null)throw new DomainException("Miembro y método obligatorios");
    boolean legacy=d.planId()!=null;
    if(legacy==(d.adminPlanId()!=null))
      throw new DomainException("Enviar exactamente uno de planId o adminPlanId");
    if(!Set.of("cash","card","transfer","other").contains(d.method()))
      throw new DomainException("Método inválido");
    int periods=d.periods()==null?1:d.periods();
    if(periods<1||periods>12)throw new DomainException("periods debe estar entre 1 y 12");
    if(idempotencyKey!=null && idempotency==null)throw new IllegalStateException("Idempotencia no configurada");

    // Acquire idempotency row before locking a member or its active subscription.
    var claim=idempotency==null?null:idempotency.claim(branch,actor.id(),"payment",idempotencyKey,
      new PaymentRequest(d.memberId(),d.planId(),d.adminPlanId(),d.method(),periods,d.notes()));
    if(claim!=null && claim.replay())return repo.receipt(branch,claim.existingResourceId())
      .orElseThrow(()->new NotFoundException("Pago de solicitud idempotente no encontrado"));

    if(!repo.activeMember(branch,d.memberId()))throw new DomainException("Miembro no activo en sucursal");
    // Neither the amount nor the duration is accepted from the frontend.
    IPaymentRepository.Plan plan=null;
    IPaymentRepository.AdminPlan adminPlan=null;
    if(legacy){
      plan=repo.plan(branch,d.planId()).orElseThrow(()->new DomainException("Plan inexistente"));
      if(!plan.active())throw new DomainException("Plan inactivo");
    }else{
      adminPlan=repo.adminPlan(branch,d.adminPlanId()).orElseThrow(()->new NotFoundException("Plan administrativo no encontrado en sucursal"));
      if(!adminPlan.active())throw new DomainException("Plan administrativo inactivo");
    }
    BigDecimal price=legacy?plan.price():adminPlan.price();
    if(price==null || price.signum()<=0)
      throw new DomainException("Un plan gratuito es informativo y no admite registro de pago");
    BigDecimal amount=price.multiply(BigDecimal.valueOf(periods));
    if(amount.compareTo(MAX_STORED_AMOUNT)>0)throw new DomainException("Importe supera el máximo admitido");

    var subscription=repo.lockActiveSubscription(branch,d.memberId());
    if(subscription.isPresent()){
      var existing=subscription.get();
      if(legacy ? !Objects.equals(existing.planId(),plan.id()) || existing.adminPlanId()!=null
                : !Objects.equals(existing.adminPlanId(),adminPlan.id()) || existing.planId()!=null)
        throw new DomainException("El miembro ya tiene un plan activo distinto");
    }
    LocalDate today=LocalDate.now(ZoneId.of(repo.gymTimezone(branch)));
    LocalDate from=subscription.map(s->s.nextDue().isAfter(today)?s.nextDue():today).orElse(today);
    LocalDate due;
    if(legacy){
      due=plan.period().equals("yearly")
        ? from.plusYears((long)plan.intervalCount()*periods)
        : from.plusMonths((long)plan.intervalCount()*periods);
    }else{
      due=from.plusDays((long)adminPlan.durationDays()*periods);
    }
    UUID sub=subscription.isPresent()?subscription.get().id()
      :(legacy?repo.createSubscription(branch,d.memberId(),plan.id(),today,due)
              :repo.createAdminSubscription(branch,d.memberId(),adminPlan.id(),today,due));
    repo.updateSubscription(sub,today,due);
    UUID paymentId=legacy
      ?repo.insertPayment(branch,d.memberId(),sub,amount,d.method(),plan.period(),periods,from,due,d.notes(),actor.id())
      :repo.insertAdminPayment(branch,d.memberId(),sub,adminPlan.id(),amount,d.method(),periods,from,due,d.notes(),actor.id());
    if(idempotency!=null)idempotency.complete(claim,paymentId);

    Map<String,Object> result=new LinkedHashMap<>();
    result.put("id",paymentId);result.put("memberId",d.memberId());result.put("subscriptionId",sub);
    result.put("amount",amount);result.put("periodStart",from);result.put("nextDueDate",due);
    result.put("periods",periods);result.put("planId",d.planId());result.put("adminPlanId",d.adminPlanId());
    result.put("planSource",legacy?"membership":"admin");
    return result;
  }
}
