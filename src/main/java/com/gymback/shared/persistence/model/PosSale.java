package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="pos_sales")
public class PosSale extends AuditedRecord {
  @Id @Column(name="id") public UUID id = UUID.randomUUID();
  @Column(name="branch_id") public UUID branchId;
  @Column(name="total") public BigDecimal total;
  @JdbcTypeCode(SqlTypes.CHAR) @Column(name="currency",columnDefinition="char(3)") public String currency = "MXN";
  @Column(name="status") public String status = "completed";
  @Column(name="sold_by") public UUID soldBy;
  @Column(name="sold_at") public OffsetDateTime soldAt = OffsetDateTime.now();
}
