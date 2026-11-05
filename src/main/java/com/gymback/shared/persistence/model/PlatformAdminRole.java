package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="platform_admins")
public class PlatformAdminRole extends AuditedRecord {
  @Id @Column(name="user_id") public UUID userId;
  @Column(name="assigned_by") public UUID assignedBy;
  @Column(name="assigned_at") public OffsetDateTime assignedAt = OffsetDateTime.now();
}
