package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="audit_events")
public class AuditEventRecord extends AuditedRecord {
  @Id @Column(name="id") public UUID id = UUID.randomUUID();
  @Column(name="gym_id") public UUID gymId;
  @Column(name="actor_user_id") public UUID actorUserId;
  @Column(name="action") public String action;
  @Column(name="entity_type") public String entityType;
  @Column(name="entity_id") public UUID entityId;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name="entity_key") public Map<String,Object> entityKey = new LinkedHashMap<>();
  @Column(name="occurred_at") public OffsetDateTime occurredAt = OffsetDateTime.now();
}
