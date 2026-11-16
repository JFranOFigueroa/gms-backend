package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="branch_settings")
public class BranchSettings extends AuditedRecord {
  @Id @Column(name="branch_id") public UUID branchId;
  @Column(name="allow_multiple_normal_classes_per_day") public Boolean allowMultipleNormalClassesPerDay;
  @Column(name="guest_booking_limit") public Integer guestBookingLimit;
  @Column(name="checkin_qr_ttl_seconds") public Integer checkinQrTtlSeconds;
}
