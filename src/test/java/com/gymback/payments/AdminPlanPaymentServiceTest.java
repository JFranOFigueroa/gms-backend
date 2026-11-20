package com.gymback.payments;

import com.gymback.modules.payments.dto.PaymentRequest;
import com.gymback.modules.payments.interfaces.IPaymentRepository;
import com.gymback.modules.payments.services.PaymentService;
import com.gymback.security.Actor;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AdminPlanPaymentServiceTest {
  @Test void rejectsMissingOrConflictingCatalogIdentifiersBeforeAnyWrite() {
    IPaymentRepository repo=mock(IPaymentRepository.class);
    AuthorizationService auth=mock(AuthorizationService.class);
    UUID branch=UUID.randomUUID(), member=UUID.randomUUID(), plan=UUID.randomUUID();
    when(auth.require(eq(branch),any(String[].class))).thenReturn(new Actor(UUID.randomUUID(),false));
    var service=new PaymentService(repo,auth);
    assertThrows(DomainException.class,()->service.register(branch,
      new PaymentRequest(member,null,null,"cash",1,null)));
    assertThrows(DomainException.class,()->service.register(branch,
      new PaymentRequest(member,plan,plan,"cash",1,null)));
    verifyNoInteractions(repo);
  }

  @Test void adminPlanPaymentTwoPeriodsUsesPriceAndCalendarDaysFromServer() {
    IPaymentRepository repo=mock(IPaymentRepository.class);
    AuthorizationService auth=mock(AuthorizationService.class);
    UUID branch=UUID.randomUUID(),member=UUID.randomUUID(),adminPlan=UUID.randomUUID();
    UUID actor=UUID.randomUUID(),sub=UUID.randomUUID(),payment=UUID.randomUUID();
    when(auth.require(eq(branch),any(String[].class))).thenReturn(new Actor(actor,false));
    when(repo.activeMember(branch,member)).thenReturn(true);
    when(repo.adminPlan(branch,adminPlan)).thenReturn(Optional.of(
      new IPaymentRepository.AdminPlan(adminPlan,30,new BigDecimal("300.00"),true)));
    when(repo.lockActiveSubscription(branch,member)).thenReturn(Optional.empty());
    when(repo.gymTimezone(branch)).thenReturn("America/Mexico_City");
    when(repo.createAdminSubscription(eq(branch),eq(member),eq(adminPlan),any(),any())).thenReturn(sub);
    when(repo.insertAdminPayment(eq(branch),eq(member),eq(sub),eq(adminPlan),eq(new BigDecimal("600.00")),
      eq("cash"),eq(2),any(),any(),eq("test"),eq(actor))).thenReturn(payment);

    Map<String,Object> response=new PaymentService(repo,auth).register(branch,
      new PaymentRequest(member,null,adminPlan,"cash",2,"test"));
    assertEquals(payment,response.get("id"));
    assertEquals(new BigDecimal("600.00"),response.get("amount"));
    assertEquals("admin",response.get("planSource"));
    assertNull(response.get("planId"));
    assertEquals(adminPlan,response.get("adminPlanId"));
    assertEquals(((LocalDate)response.get("periodStart")).plusDays(60),response.get("nextDueDate"));
    verify(repo).updateSubscription(sub,LocalDate.now(ZoneId.of("America/Mexico_City")),
      (LocalDate)response.get("nextDueDate"));
  }

  @Test void adminPlanUsesFutureDueDateToRenewNotToday() {
    IPaymentRepository repo=mock(IPaymentRepository.class);
    AuthorizationService auth=mock(AuthorizationService.class);
    UUID branch=UUID.randomUUID(),member=UUID.randomUUID(),adminPlan=UUID.randomUUID();
    UUID actor=UUID.randomUUID(),sub=UUID.randomUUID();
    LocalDate future=LocalDate.now(ZoneId.of("America/Mexico_City")).plusDays(10);
    when(auth.require(eq(branch),any(String[].class))).thenReturn(new Actor(actor,false));
    when(repo.activeMember(branch,member)).thenReturn(true);
    when(repo.adminPlan(branch,adminPlan)).thenReturn(Optional.of(
      new IPaymentRepository.AdminPlan(adminPlan,15,new BigDecimal("300.00"),true)));
    when(repo.lockActiveSubscription(branch,member)).thenReturn(Optional.of(
      new IPaymentRepository.Subscription(sub,null,adminPlan,future.minusDays(20),future)));
    when(repo.gymTimezone(branch)).thenReturn("America/Mexico_City");
    Map<String,Object> response=new PaymentService(repo,auth).register(branch,
      new PaymentRequest(member,null,adminPlan,"cash",1,null));
    assertEquals(future,response.get("periodStart"));
    assertEquals(future.plusDays(15),response.get("nextDueDate"));
    verify(repo,never()).createAdminSubscription(any(),any(),any(),any(),any());
  }

  @Test void rejectsInactiveOrCrossBranchAdminPlanWithoutWriting() {
    IPaymentRepository repo=mock(IPaymentRepository.class);
    AuthorizationService auth=mock(AuthorizationService.class);
    UUID branch=UUID.randomUUID(),member=UUID.randomUUID(),plan=UUID.randomUUID();
    when(auth.require(eq(branch),any(String[].class))).thenReturn(new Actor(UUID.randomUUID(),false));
    when(repo.activeMember(branch,member)).thenReturn(true);
    when(repo.adminPlan(branch,plan)).thenReturn(Optional.of(
      new IPaymentRepository.AdminPlan(plan,30,new BigDecimal("300.00"),false)));
    var service=new PaymentService(repo,auth);
    assertThrows(DomainException.class,()->service.register(branch,
      new PaymentRequest(member,null,plan,"cash",1,null)));
    when(repo.adminPlan(branch,plan)).thenReturn(Optional.empty());
    assertThrows(NotFoundException.class,()->service.register(branch,
      new PaymentRequest(member,null,plan,"cash",1,null)));
    verify(repo,never()).createAdminSubscription(any(),any(),any(),any(),any());
    verify(repo,never()).insertAdminPayment(any(),any(),any(),any(),any(),any(),anyInt(),any(),any(),any(),any());
  }

  @Test void rejectsChangeToDifferentActiveSubscriptionCatalog() {
    IPaymentRepository repo=mock(IPaymentRepository.class);
    AuthorizationService auth=mock(AuthorizationService.class);
    UUID branch=UUID.randomUUID(),member=UUID.randomUUID(),plan=UUID.randomUUID();
    UUID legacy=UUID.randomUUID();
    when(auth.require(eq(branch),any(String[].class))).thenReturn(new Actor(UUID.randomUUID(),false));
    when(repo.activeMember(branch,member)).thenReturn(true);
    when(repo.adminPlan(branch,plan)).thenReturn(Optional.of(
      new IPaymentRepository.AdminPlan(plan,30,new BigDecimal("300.00"),true)));
    when(repo.lockActiveSubscription(branch,member)).thenReturn(Optional.of(
      new IPaymentRepository.Subscription(UUID.randomUUID(),legacy,null,LocalDate.now(),LocalDate.now().plusDays(3))));
    assertThrows(DomainException.class,()->new PaymentService(repo,auth).register(branch,
      new PaymentRequest(member,null,plan,"cash",1,null)));
    verify(repo,never()).insertAdminPayment(any(),any(),any(),any(),any(),any(),anyInt(),any(),any(),any(),any());
  }

  @Test void legacyRequestStillUsesOldConstructorAndMonthlyCalculations() {
    IPaymentRepository repo=mock(IPaymentRepository.class);
    AuthorizationService auth=mock(AuthorizationService.class);
    UUID branch=UUID.randomUUID(),member=UUID.randomUUID(),plan=UUID.randomUUID();
    when(auth.require(eq(branch),any(String[].class))).thenReturn(new Actor(UUID.randomUUID(),false));
    when(repo.activeMember(branch,member)).thenReturn(true);
    when(repo.plan(branch,plan)).thenReturn(Optional.of(
      new IPaymentRepository.Plan(plan,"monthly",1,new BigDecimal("450.00"),true)));
    when(repo.lockActiveSubscription(branch,member)).thenReturn(Optional.empty());
    when(repo.gymTimezone(branch)).thenReturn("America/Mexico_City");
    var response=new PaymentService(repo,auth).register(branch,new PaymentRequest(member,plan,"cash",1,null));
    assertEquals("membership",response.get("planSource"));
    assertEquals(((LocalDate)response.get("periodStart")).plusMonths(1),response.get("nextDueDate"));
  }
}
