package com.gymback.modules.calendar.model;
import com.gymback.shared.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name="class_templates")
public class ClassTemplate implements BaseEntity {
  @Id @Column(name="id",nullable=false) private UUID id = UUID.randomUUID();
  @Column(name="gym_id") private UUID gymId;
    @Column(name="name")
  private String name;
  @Column(name="category")
  private String category = "normal";
  @Column(name="color")
  private String color;
  @Column(name="duration_minutes")
  private Integer durationMinutes = 60;
  @Column(name="capacity")
  private Integer capacity;
  @Column(name="is_active")
  private Boolean isActive = true;
  @Column(name="created_at")
  private OffsetDateTime createdAt = OffsetDateTime.now();
  @Override public UUID getId() {return id;}
  public void setId(UUID id){this.id=id;}
  @Override public UUID getGymId(){return gymId;} public void setGymId(UUID id){this.gymId=id;}
    public String getName() {return name;}
  public void setName(String value) {this.name=value;}
  public String getCategory() {return category;}
  public void setCategory(String value) {this.category=value;}
  public String getColor() {return color;}
  public void setColor(String value) {this.color=value;}
  public Integer getDurationMinutes() {return durationMinutes;}
  public void setDurationMinutes(Integer value) {this.durationMinutes=value;}
  public Integer getCapacity() {return capacity;}
  public void setCapacity(Integer value) {this.capacity=value;}
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
