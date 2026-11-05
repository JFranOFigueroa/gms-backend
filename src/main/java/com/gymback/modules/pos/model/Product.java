package com.gymback.modules.pos.model;
import com.gymback.shared.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name="products")
public class Product implements BaseEntity {
  @Id @Column(name="id",nullable=false) private UUID id = UUID.randomUUID();
  @Column(name="gym_id") private UUID gymId;
    @Column(name="sku")
  private String sku;
  @Column(name="barcode")
  private String barcode;
  @Column(name="name")
  private String name;
  @Column(name="category")
  private String category;
  @Column(name="price")
  private BigDecimal price;
  @Column(name="stock")
  private Integer stock = 0;
  @Column(name="low_stock_threshold")
  private Integer lowStockThreshold = 5;
  @Column(name="is_active")
  private Boolean isActive = true;
  @Column(name="created_at")
  private OffsetDateTime createdAt = OffsetDateTime.now();
  @Column(name="updated_at")
  private OffsetDateTime updatedAt = OffsetDateTime.now();
  @Override public UUID getId() {return id;}
  public void setId(UUID id){this.id=id;}
  @Override public UUID getGymId(){return gymId;} public void setGymId(UUID id){this.gymId=id;}
    public String getSku() {return sku;}
  public void setSku(String value) {this.sku=value;}
  public String getBarcode() {return barcode;}
  public void setBarcode(String value) {this.barcode=value;}
  public String getName() {return name;}
  public void setName(String value) {this.name=value;}
  public String getCategory() {return category;}
  public void setCategory(String value) {this.category=value;}
  public BigDecimal getPrice() {return price;}
  public void setPrice(BigDecimal value) {this.price=value;}
  public Integer getStock() {return stock;}
  public void setStock(Integer value) {this.stock=value;}
  public Integer getLowStockThreshold() {return lowStockThreshold;}
  public void setLowStockThreshold(Integer value) {this.lowStockThreshold=value;}
  public Boolean getIsActive() {return isActive;}
  public void setIsActive(Boolean value) {this.isActive=value;}
  public OffsetDateTime getCreatedAt() {return createdAt;}
  public void setCreatedAt(OffsetDateTime value) {this.createdAt=value;}
  public OffsetDateTime getUpdatedAt() {return updatedAt;}
  public void setUpdatedAt(OffsetDateTime value) {this.updatedAt=value;}

  @Column(name="created_by") private UUID createdBy;
  @Column(name="updated_by") private UUID updatedBy;
  @Column(name="deleted_at") private OffsetDateTime deletedAt;
  @Column(name="deleted_by") private UUID deletedBy;
  public UUID getCreatedBy() {return createdBy;}
  public void setCreatedBy(UUID value) {this.createdBy=value;}
  public UUID getUpdatedBy() {return updatedBy;}
  public void setUpdatedBy(UUID value) {this.updatedBy=value;}
  public OffsetDateTime getDeletedAt() {return deletedAt;}
  public void setDeletedAt(OffsetDateTime value) {this.deletedAt=value;}
  public UUID getDeletedBy() {return deletedBy;}
  public void setDeletedBy(UUID value) {this.deletedBy=value;}
}
