package com.gymback.modules.plans.model;
import com.gymback.shared.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name="membership_plans")
public class MembershipPlan implements BaseEntity {
  @Id @Column(name="id",nullable=false) private UUID id = UUID.randomUUID();
  @Column(name="gym_id") private UUID gymId;
    @Column(name="name")
  private String name;
  @Column(name="billing_period")
  private String billingPeriod = "monthly";
  @Column(name="interval_count")
  private Integer intervalCount = 1;
  @Column(name="price")
  private BigDecimal price;
  @Column(name="description")
  private String description;
  @Column(name="is_active")
  private Boolean isActive = true;
  @Column(name="created_at")
  private OffsetDateTime createdAt = OffsetDateTime.now();
  @Override public UUID getId() {return id;}
  public void setId(UUID id){this.id=id;}
  @Override public UUID getGymId(){return gymId;} public void setGymId(UUID id){this.gymId=id;}
    public String getName() {return name;}
  public void setName(String value) {this.name=value;}
  public String getBillingPeriod() {return billingPeriod;}
  public void setBillingPeriod(String value) {this.billingPeriod=value;}
  public Integer getIntervalCount() {return intervalCount;}
  public void setIntervalCount(Integer value) {this.intervalCount=value;}
  public BigDecimal getPrice() {return price;}
  public void setPrice(BigDecimal value) {this.price=value;}
  public String getDescription() {return description;}
  public void setDescription(String value) {this.description=value;}
  public Boolean getIsActive() {return isActive;}
  public void setIsActive(Boolean value) {this.isActive=value;}
  public OffsetDateTime getCreatedAt() {return createdAt;}
  public void setCreatedAt(OffsetDateTime value) {this.createdAt=value;}

  @Column(name="created_by") private UUID createdBy;
  @Column(name="updated_at") private OffsetDateTime updatedAt = OffsetDateTime.now(java.time.ZoneOffset.UTC);
  @Column(name="updated_by") private UUID updatedBy;
  @Column(name="deleted_at") private OffsetDateTime deletedAt;
  @Column(name="deleted_by") private UUID deletedBy;
  public UUID getCreatedBy() {return createdBy;}
  public void setCreatedBy(UUID value) {this.createdBy=value;}
  public OffsetDateTime getUpdatedAt() {return updatedAt;}
  public void setUpdatedAt(OffsetDateTime value) {this.updatedAt=value;}
  public UUID getUpdatedBy() {return updatedBy;}
  public void setUpdatedBy(UUID value) {this.updatedBy=value;}
  public OffsetDateTime getDeletedAt() {return deletedAt;}
  public void setDeletedAt(OffsetDateTime value) {this.deletedAt=value;}
  public UUID getDeletedBy() {return deletedBy;}
  public void setDeletedBy(UUID value) {this.deletedBy=value;}
}
