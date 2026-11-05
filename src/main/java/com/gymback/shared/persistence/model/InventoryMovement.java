package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="inventory_movements")
public class InventoryMovement extends AuditedRecord {
  @Id @Column(name="id") public UUID id = UUID.randomUUID();
  @Column(name="gym_id") public UUID gymId;
  @Column(name="product_id") public UUID productId;
  @Column(name="stock_delta") public Integer stockDelta;
  @Column(name="reason") public String reason;
  @Column(name="sale_id") public UUID saleId;
  @Column(name="notes") public String notes;
  @Column(name="performed_by") public UUID performedBy;
}
