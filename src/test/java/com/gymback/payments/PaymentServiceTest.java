package com.gymback.payments;

import com.gymback.modules.payments.dto.PaymentRequest;
import com.gymback.modules.payments.interfaces.IPaymentRepository;
import com.gymback.modules.payments.services.PaymentService;
import com.gymback.security.Actor;
import com.gymback.security.AuthorizationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
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
