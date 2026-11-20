package com.gymback.modules.payments.interfaces;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public interface IPaymentRepository {
  record Plan(UUID id,String period,int intervalCount,BigDecimal price,boolean active) {}
  record AdminPlan(UUID id,int durationDays,BigDecimal price,boolean active) {}
  record Subscription(UUID id,UUID planId,UUID adminPlanId,LocalDate startedOn,LocalDate nextDue) {
    public Subscription(UUID id,UUID planId,LocalDate startedOn,LocalDate nextDue) {
      this(id,planId,null,startedOn,nextDue);
    }
  }
  Optional<Plan> plan(UUID branch,UUID planId);
  Optional<AdminPlan> adminPlan(UUID branch,UUID adminPlanId);
  Optional<Subscription> lockActiveSubscription(UUID branch,UUID member);
  boolean activeMember(UUID branch,UUID member);
  String gymTimezone(UUID branch);
  UUID createSubscription(UUID branch,UUID member,UUID plan,LocalDate started,LocalDate due);
  UUID createAdminSubscription(UUID branch,UUID member,UUID adminPlan,LocalDate started,LocalDate due);
  void updateSubscription(UUID id,LocalDate lastPayment,LocalDate nextDue);
  UUID insertPayment(UUID branch,UUID member,UUID subscription,BigDecimal amount,String method,String periodicity,int periods,LocalDate periodStart,LocalDate nextDue,String notes,UUID actor);
  UUID insertAdminPayment(UUID branch,UUID member,UUID subscription,UUID adminPlan,BigDecimal amount,String method,int periods,LocalDate periodStart,LocalDate nextDue,String notes,UUID actor);
  Optional<Map<String,Object>> receipt(UUID branch,UUID paymentId);
  List<Map<String,Object>> list(UUID branch,UUID memberId,int limit);
}
