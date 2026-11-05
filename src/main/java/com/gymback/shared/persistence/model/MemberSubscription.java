package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="member_subscriptions")
public class MemberSubscription extends AuditedRecord {
  @Id @Column(name="id") public UUID id = UUID.randomUUID();
  @Column(name="gym_id") public UUID gymId;
  @Column(name="member_id") public UUID memberId;
  @Column(name="plan_id") public UUID planId;
  @Column(name="started_on") public LocalDate startedOn;
  @Column(name="next_due_date") public LocalDate nextDueDate;
  @Column(name="last_payment_date") public LocalDate lastPaymentDate;
  @Column(name="status") public String status = "active";
}
