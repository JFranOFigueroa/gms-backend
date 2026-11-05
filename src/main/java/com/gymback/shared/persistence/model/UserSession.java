package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="user_sessions")
public class UserSession extends AuditedRecord {
  @Id @Column(name="id") public UUID id = UUID.randomUUID();
  @Column(name="user_id") public UUID userId;
  @Column(name="refresh_token_hash") public String refreshTokenHash;
  @Column(name="issued_at") public OffsetDateTime issuedAt = OffsetDateTime.now();
  @Column(name="expires_at") public OffsetDateTime expiresAt;
  @Column(name="revoked_at") public OffsetDateTime revokedAt;
}
