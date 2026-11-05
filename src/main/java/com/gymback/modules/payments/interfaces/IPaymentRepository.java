package com.gymback.modules.payments.interfaces;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
public interface IPaymentRepository {
  record Plan(UUID id,String period,int intervalCount,BigDecimal price,boolean active){}
  record Subscription(UUID id,UUID planId,LocalDate startedOn,LocalDate nextDue){}
  Optional<Plan> plan(UUID gym,UUID planId);
  Optional<Subscription> lockActiveSubscription(UUID gym,UUID member);
  boolean activeMember(UUID gym,UUID member);
  String gymTimezone(UUID gym);
  UUID createSubscription(UUID gym,UUID member,UUID plan,LocalDate started,LocalDate due);
  void updateSubscription(UUID id,LocalDate lastPayment,LocalDate nextDue);
  UUID insertPayment(UUID gym,UUID member,UUID subscription,BigDecimal amount,String method,String periodicity,int periods,LocalDate periodStart,LocalDate nextDue,String notes,UUID actor);
  List<Map<String,Object>> list(UUID gym,UUID memberId,int limit);
}
