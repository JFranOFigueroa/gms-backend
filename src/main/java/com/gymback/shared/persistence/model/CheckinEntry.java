package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="checkin_events")
public class CheckinEntry extends AuditedRecord {
  @Id @Column(name="id") public UUID id = UUID.randomUUID();
  @Column(name="gym_id") public UUID gymId;
  @Column(name="member_id") public UUID memberId;
  @Column(name="guest_pass_id") public UUID guestPassId;
  @Column(name="staff_user_id") public UUID staffUserId;
  @Column(name="device_id") public UUID deviceId;
  @Column(name="method") public String method = "manual";
  @Column(name="allowed") public Boolean allowed;
  @Column(name="reason_code") public String reasonCode;
  @Column(name="reason_detail") public String reasonDetail;
  @Column(name="visit_date") public LocalDate visitDate;
  @Column(name="occurred_at") public OffsetDateTime occurredAt = OffsetDateTime.now();
}
