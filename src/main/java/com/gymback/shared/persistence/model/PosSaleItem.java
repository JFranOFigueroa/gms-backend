package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.util.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="pos_sale_items")
public class PosSaleItem extends AuditedRecord {
  @Id @Column(name="id") public UUID id = UUID.randomUUID();
  @Column(name="gym_id") public UUID gymId;
  @Column(name="sale_id") public UUID saleId;
  @Column(name="product_id") public UUID productId;
  @Column(name="product_name") public String productName;
  @Column(name="quantity") public Integer quantity;
  @Column(name="unit_price") public BigDecimal unitPrice;
  @Column(name="line_total",insertable=false,updatable=false) public BigDecimal lineTotal;
}
