package com.gymback.modules.payments.dto;
import java.util.UUID;
public record PaymentRequest(UUID memberId,UUID planId,String method,Integer periods,String notes) {}
