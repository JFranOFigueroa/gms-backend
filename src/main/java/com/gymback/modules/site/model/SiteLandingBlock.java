package com.gymback.modules.site.model;

import com.gymback.shared.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.*;

/** Bloque de la landing única del sitio. No tiene branch_id. */
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "site_landing_blocks")
public class SiteLandingBlock implements BaseEntity {
  @Id @Column(name = "id", nullable = false) private UUID id = UUID.randomUUID();
  @Column(name = "block_type", nullable = false) private String blockType;
  @Column(name = "sort_order", nullable = false) private Integer sortOrder;
  @Column(name = "enabled", nullable = false) private Boolean enabled = true;
  @Column(name = "block_data", nullable = false)
  @JdbcTypeCode(SqlTypes.JSON) private Map<String, Object> blockData = new LinkedHashMap<>();
  @Column(name = "created_at") private OffsetDateTime createdAt = OffsetDateTime.now();
  @Column(name = "updated_at") private OffsetDateTime updatedAt = OffsetDateTime.now();
  @Column(name = "created_by") private UUID createdBy;
  @Column(name = "updated_by") private UUID updatedBy;
  @Column(name = "deleted_at") private OffsetDateTime deletedAt;
  @Column(name = "deleted_by") private UUID deletedBy;

  @Override public UUID getId() { return id; }
  public void setId(UUID value) { this.id = value; }
  public String getBlockType() { return blockType; }
  public void setBlockType(String value) { this.blockType = value; }
  public Integer getSortOrder() { return sortOrder; }
  public void setSortOrder(Integer value) { this.sortOrder = value; }
  public Boolean getEnabled() { return enabled; }
  public void setEnabled(Boolean value) { this.enabled = value; }
  public Map<String, Object> getBlockData() { return blockData; }
  public void setBlockData(Map<String, Object> value) { this.blockData = value; }
  public OffsetDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(OffsetDateTime value) { this.createdAt = value; }
  public OffsetDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(OffsetDateTime value) { this.updatedAt = value; }
  public UUID getCreatedBy() { return createdBy; }
  public void setCreatedBy(UUID value) { this.createdBy = value; }
  public UUID getUpdatedBy() { return updatedBy; }
  public void setUpdatedBy(UUID value) { this.updatedBy = value; }
  public OffsetDateTime getDeletedAt() { return deletedAt; }
  public void setDeletedAt(OffsetDateTime value) { this.deletedAt = value; }
  public UUID getDeletedBy() { return deletedBy; }
  public void setDeletedBy(UUID value) { this.deletedBy = value; }
}
