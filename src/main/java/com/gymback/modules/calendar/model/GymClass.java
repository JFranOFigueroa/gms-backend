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
@Table(name="gym_classes")
public class GymClass implements BaseEntity {
  @Id @Column(name="id",nullable=false) private UUID id = UUID.randomUUID();
  @Column(name="gym_id") private UUID gymId;
    @Column(name="template_id")
  private UUID templateId;
  @Column(name="name")
  private String name;
  @Column(name="category")
  private String category = "normal";
  @Column(name="color")
  private String color;
  @Column(name="starts_at")
  private OffsetDateTime startsAt;
  @Column(name="ends_at")
  private OffsetDateTime endsAt;
  @Column(name="capacity")
  private Integer capacity;
  @Column(name="trainer_user_id")
  private UUID trainerUserId;
  @Column(name="status")
  private String status = "scheduled";
  @Column(name="created_by")
  private UUID createdBy;
  @Column(name="created_at")
  private OffsetDateTime createdAt = OffsetDateTime.now();
  @Override public UUID getId() {return id;}
  public void setId(UUID id){this.id=id;}
  @Override public UUID getGymId(){return gymId;} public void setGymId(UUID id){this.gymId=id;}
    public UUID getTemplateId() {return templateId;}
  public void setTemplateId(UUID value) {this.templateId=value;}
  public String getName() {return name;}
  public void setName(String value) {this.name=value;}
  public String getCategory() {return category;}
  public void setCategory(String value) {this.category=value;}
  public String getColor() {return color;}
  public void setColor(String value) {this.color=value;}
  public OffsetDateTime getStartsAt() {return startsAt;}
  public void setStartsAt(OffsetDateTime value) {this.startsAt=value;}
  public OffsetDateTime getEndsAt() {return endsAt;}
  public void setEndsAt(OffsetDateTime value) {this.endsAt=value;}
  public Integer getCapacity() {return capacity;}
  public void setCapacity(Integer value) {this.capacity=value;}
  public UUID getTrainerUserId() {return trainerUserId;}
  public void setTrainerUserId(UUID value) {this.trainerUserId=value;}
  public String getStatus() {return status;}
  public void setStatus(String value) {this.status=value;}
  public UUID getCreatedBy() {return createdBy;}
  public void setCreatedBy(UUID value) {this.createdBy=value;}
  public OffsetDateTime getCreatedAt() {return createdAt;}
  public void setCreatedAt(OffsetDateTime value) {this.createdAt=value;}

  @Column(name="updated_at") private OffsetDateTime updatedAt = OffsetDateTime.now(java.time.ZoneOffset.UTC);
  @Column(name="updated_by") private UUID updatedBy;
  @Column(name="deleted_at") private OffsetDateTime deletedAt;
  @Column(name="deleted_by") private UUID deletedBy;
  public OffsetDateTime getUpdatedAt() {return updatedAt;}
  public void setUpdatedAt(OffsetDateTime value) {this.updatedAt=value;}
  public UUID getUpdatedBy() {return updatedBy;}
  public void setUpdatedBy(UUID value) {this.updatedBy=value;}
  public OffsetDateTime getDeletedAt() {return deletedAt;}
  public void setDeletedAt(OffsetDateTime value) {this.deletedAt=value;}
  public UUID getDeletedBy() {return deletedBy;}
  public void setDeletedBy(UUID value) {this.deletedBy=value;}
}
