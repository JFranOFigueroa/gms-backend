package com.gymback.modules.payments.dto;

import java.util.UUID;

/** Exactly one catalog identifier is required. */
public record PaymentRequest(
    UUID memberId,
    UUID planId,
    UUID adminPlanId,
    String method,
    Integer periods,
    String notes
) {
  /** Compatibility with existing Java callers using the legacy five-argument request. */
  public PaymentRequest(UUID memberId, UUID planId, String method, Integer periods, String notes) {
    this(memberId, planId, null, method, periods, notes);
  }
}
