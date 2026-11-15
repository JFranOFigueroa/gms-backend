package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@IdClass(BranchModule.Key.class)
@Entity @Table(name="branch_modules")
public class BranchModule extends AuditedRecord {
  @Id @Column(name="branch_id") public UUID branchId;
  @Id @Column(name="module_key") public String moduleKey;
  @Column(name="enabled") public Boolean enabled = true;
  @JdbcTypeCode(SqlTypes.JSON) @Column(name="options") public Map<String,Object> options = new LinkedHashMap<>();
  public static class Key implements Serializable {
    public UUID branchId;
    public String moduleKey;
    public Key() {}
    @Override public boolean equals(Object o) { if(!(o instanceof Key k))return false;return Objects.equals(branchId,k.branchId) && Objects.equals(moduleKey,k.moduleKey); }
    @Override public int hashCode() {return Objects.hash(branchId,moduleKey);}
  }
}
