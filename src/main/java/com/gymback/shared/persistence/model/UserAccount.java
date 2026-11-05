package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="users")
public class UserAccount extends AuditedRecord {
  @Id @Column(name="id") public UUID id = UUID.randomUUID();
  @Column(name="full_name") public String fullName;
  @Column(name="email",columnDefinition="citext") public String email;
  @Column(name="phone") public String phone;
  @Column(name="password_hash") public String passwordHash;
  @Column(name="status") public String status = "invited";
  @Column(name="default_gym_id") public UUID defaultGymId;
  @Column(name="email_verified_at") public OffsetDateTime emailVerifiedAt;
}
