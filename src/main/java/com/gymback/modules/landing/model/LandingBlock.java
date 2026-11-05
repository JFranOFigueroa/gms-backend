package com.gymback.modules.landing.model;
import com.gymback.shared.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name="landing_blocks")
public class LandingBlock implements BaseEntity {
  @Id @Column(name="id",nullable=false) private UUID id = UUID.randomUUID();
  @Column(name="gym_id") private UUID gymId;
    @Column(name="block_type")
  private String blockType;
  @Column(name="sort_order")
  private Integer sortOrder;
  @Column(name="enabled")
  private Boolean enabled = true;
  @Column(name="block_data")
  @JdbcTypeCode(SqlTypes.JSON)
  private Map<String,Object> blockData = new LinkedHashMap<>();
  @Column(name="created_at")
  private OffsetDateTime createdAt = OffsetDateTime.now();
  @Column(name="updated_at")
  private OffsetDateTime updatedAt = OffsetDateTime.now();
  @Override public UUID getId() {return id;}
  public void setId(UUID id){this.id=id;}
  @Override public UUID getGymId(){return gymId;} public void setGymId(UUID id){this.gymId=id;}
    public String getBlockType() {return blockType;}
  public void setBlockType(String value) {this.blockType=value;}
  public Integer getSortOrder() {return sortOrder;}
  public void setSortOrder(Integer value) {this.sortOrder=value;}
  public Boolean getEnabled() {return enabled;}
  public void setEnabled(Boolean value) {this.enabled=value;}
  public Map<String,Object> getBlockData() {return blockData;}
  public void setBlockData(Map<String,Object> value) {this.blockData=value;}
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
