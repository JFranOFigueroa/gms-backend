package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="membership_payments")
public class PaymentEntry extends AuditedRecord {
  @Id @Column(name="id") public UUID id = UUID.randomUUID();
  @Column(name="gym_id") public UUID gymId;
  @Column(name="member_id") public UUID memberId;
  @Column(name="subscription_id") public UUID subscriptionId;
  @Column(name="amount") public BigDecimal amount;
  @Column(name="method") public String method;
  @Column(name="concept") public String concept = "membership";
  @Column(name="periodicity") public String periodicity;
  @Column(name="periods") public Integer periods = 1;
  @Column(name="paid_at") public OffsetDateTime paidAt = OffsetDateTime.now();
  @Column(name="period_start") public LocalDate periodStart;
  @Column(name="next_due_date") public LocalDate nextDueDate;
  @Column(name="status") public String status = "paid";
  @Column(name="external_reference") public String externalReference;
  @Column(name="notes") public String notes;
  @Column(name="registered_by") public UUID registeredBy;
}
