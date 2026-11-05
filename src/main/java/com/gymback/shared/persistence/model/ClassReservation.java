package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="class_reservations")
public class ClassReservation extends AuditedRecord {
  @Id @Column(name="id") public UUID id = UUID.randomUUID();
  @Column(name="gym_id") public UUID gymId;
  @Column(name="class_id") public UUID classId;
  @Column(name="user_id") public UUID userId;
  @Column(name="status") public String status = "booked";
  @Column(name="booked_at") public OffsetDateTime bookedAt = OffsetDateTime.now();
  @Column(name="cancelled_at") public OffsetDateTime cancelledAt;
}
