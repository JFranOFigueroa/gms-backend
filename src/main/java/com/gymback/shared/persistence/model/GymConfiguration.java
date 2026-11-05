package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="gym_settings")
public class GymConfiguration extends AuditedRecord {
  @Id @Column(name="gym_id") public UUID gymId;
  @Column(name="theme_preset") public String themePreset;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name="theme_overrides") public Map<String,Object> themeOverrides = new LinkedHashMap<>();
  @Column(name="allow_multiple_normal_classes_per_day") public Boolean allowMultipleNormalClassesPerDay;
  @Column(name="guest_booking_limit") public Integer guestBookingLimit;
  @Column(name="checkin_qr_ttl_seconds") public Integer checkinQrTtlSeconds;
  @Column(name="landing_published") public Boolean landingPublished;
}
