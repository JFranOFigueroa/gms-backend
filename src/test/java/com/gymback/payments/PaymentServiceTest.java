package com.gymback.payments;

import com.gymback.modules.payments.dto.PaymentRequest;
import com.gymback.modules.payments.interfaces.IPaymentRepository;
import com.gymback.modules.payments.services.PaymentService;
import com.gymback.security.Actor;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.services.IdempotencyService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    @Test
    void repeatingSamePaymentKeyDoesNotCreateSecondPayment() {
        IPaymentRepository repo=mock(IPaymentRepository.class);
        AuthorizationService auth=mock(AuthorizationService.class);
        IdempotencyService idem=mock(IdempotencyService.class);
        UUID gym=UUID.randomUUID(),actor=UUID.randomUUID(),member=UUID.randomUUID();
        UUID plan=UUID.randomUUID(),existing=UUID.randomUUID();
        var input=new PaymentRequest(member,plan,"cash",1,null);
        when(auth.require(eq(gym),any(String[].class))).thenReturn(new Actor(actor,false));
        when(idem.claim(eq(gym),eq(actor),eq("payment"),eq("same-key-123"),any()))
          .thenReturn(new IdempotencyService.Claim(gym,actor,"payment","same-key-123",existing));
        when(repo.receipt(gym,existing)).thenReturn(Optional.of(java.util.Map.of("id",existing)));
        var result=new PaymentService(repo,auth,idem).register(gym,input,"same-key-123");
        assertEquals(existing,result.get("id"));
        verify(repo,never()).insertPayment(any(),any(),any(),any(),any(),any(),
          org.mockito.ArgumentMatchers.anyInt(),any(),any(),any(),any());
    }

    @Test
    void freeInformationalPlanReturnsBusinessErrorBeforeAnySubscriptionChanges() {
        IPaymentRepository repo=mock(IPaymentRepository.class);
        AuthorizationService auth=mock(AuthorizationService.class);
        UUID gym=UUID.randomUUID(),member=UUID.randomUUID(),plan=UUID.randomUUID();
        when(auth.require(eq(gym),any(String[].class))).thenReturn(new Actor(UUID.randomUUID(),false));
        when(repo.activeMember(gym,member)).thenReturn(true);
        when(repo.plan(gym,plan)).thenReturn(Optional.of(
            new IPaymentRepository.Plan(plan,"monthly",1,BigDecimal.ZERO,true)));
        assertThrows(com.gymback.shared.exceptions.DomainException.class,()->new PaymentService(repo,auth)
            .register(gym,new PaymentRequest(member,plan,"cash",1,null)));
        verify(repo,never()).createSubscription(eq(gym),eq(member),eq(plan),any(),any());
    }

    @Test
    void computesAmountServerSide() {
        IPaymentRepository repo = mock(IPaymentRepository.class);
        AuthorizationService auth = mock(AuthorizationService.class);

        UUID gym = UUID.randomUUID();
        UUID member = UUID.randomUUID();
        UUID plan = UUID.randomUUID();
        UUID actor = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        when(auth.require(eq(gym), any(String[].class))).thenReturn(new Actor(actor, false));
        when(repo.activeMember(gym, member)).thenReturn(true);
        when(repo.plan(gym, plan)).thenReturn(Optional.of(
            new IPaymentRepository.Plan(plan, "monthly", 1, new BigDecimal("450.00"), true)
        ));
        when(repo.lockActiveSubscription(gym, member)).thenReturn(Optional.empty());
        when(repo.gymTimezone(gym)).thenReturn("America/Mexico_City");
        when(repo.createSubscription(
            eq(gym), eq(member), eq(plan), any(LocalDate.class), any(LocalDate.class)
        )).thenReturn(subscriptionId);
        when(repo.insertPayment(
            eq(gym), eq(member), eq(subscriptionId), eq(new BigDecimal("900.00")),
            eq("cash"), eq("monthly"), eq(2), any(LocalDate.class),
            any(LocalDate.class), eq("test"), eq(actor)
        )).thenReturn(paymentId);

        var result = new PaymentService(repo, auth).register(
            gym, new PaymentRequest(member, plan, "cash", 2, "test")
        );

        assertEquals(paymentId, result.get("id"));
        assertEquals(new BigDecimal("900.00"), result.get("amount"));
        assertEquals(subscriptionId, result.get("subscriptionId"));
    }
}
