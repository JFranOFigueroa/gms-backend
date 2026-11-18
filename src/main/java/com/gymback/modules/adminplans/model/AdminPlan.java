package com.gymback.modules.adminplans.model;

import com.gymback.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "branch_admin_plans")
@SQLRestriction("deleted_at IS NULL")
public class AdminPlan implements BaseEntity {
  @Id
  @Column(name = "id", nullable = false)
  private UUID id = UUID.randomUUID();

  @Column(name = "branch_id", nullable = false)
  private UUID branchId;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "price", nullable = false, precision = 12, scale = 2)
  private BigDecimal price;

  @Column(name = "duration_days", nullable = false)
  private Integer durationDays;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @Column(name = "updated_by")
  private UUID updatedBy;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  @Column(name = "deleted_by")
  private UUID deletedBy;

  @Override public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }
  @Override public UUID getBranchId() { return branchId; }
  public void setBranchId(UUID branchId) { this.branchId = branchId; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public Integer getDurationDays() { return durationDays; }
  public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean active) { isActive = active; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
  public UUID getCreatedBy() { return createdBy; }
  public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
  public OffsetDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
  public UUID getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
  public OffsetDateTime getDeletedAt() { return deletedAt; }
  public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
  public UUID getDeletedBy() { return deletedBy; }
  public void setDeletedBy(UUID deletedBy) { this.deletedBy = deletedBy; }
}
