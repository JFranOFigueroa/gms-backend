package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@IdClass(GymStaffRole.Key.class)
@Entity @Table(name="gym_staff_roles")
public class GymStaffRole extends AuditedRecord {
  @Id @Column(name="gym_id") public UUID gymId;
  @Id @Column(name="user_id") public UUID userId;
  @Id @Column(name="role") public String role;
  @Column(name="is_active") public Boolean isActive = true;
  @Column(name="assigned_by") public UUID assignedBy;
  @Column(name="assigned_at") public OffsetDateTime assignedAt = OffsetDateTime.now();
  public static class Key implements Serializable {
    public UUID gymId;
    public UUID userId;
    public String role;
    public Key() {}
    @Override public boolean equals(Object o) { if(!(o instanceof Key k))return false;return Objects.equals(gymId,k.gymId) && Objects.equals(userId,k.userId) && Objects.equals(role,k.role); }
    @Override public int hashCode() {return Objects.hash(gymId,userId,role);}
  }
}
