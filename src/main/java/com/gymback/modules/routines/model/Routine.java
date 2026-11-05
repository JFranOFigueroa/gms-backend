package com.gymback.modules.routines.model;
import com.gymback.shared.domain.BaseEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name="routines")
public class Routine implements BaseEntity {
  @Id @Column(name="id",nullable=false) private UUID id = UUID.randomUUID();
  @Column(name="gym_id") private UUID gymId;
    @Column(name="name")
  private String name;
  @Column(name="description")
  private String description;
  @Column(name="level")
  private String level = "beginner";
  @Column(name="goal")
  private String goal;
  @Column(name="published")
  private Boolean published = false;
  @Column(name="created_by")
  private UUID createdBy;
  @Column(name="created_at")
  private OffsetDateTime createdAt = OffsetDateTime.now();
  @Column(name="updated_at")
  private OffsetDateTime updatedAt = OffsetDateTime.now();
  @Override public UUID getId() {return id;}
  public void setId(UUID id){this.id=id;}
  @Override public UUID getGymId(){return gymId;} public void setGymId(UUID id){this.gymId=id;}
    public String getName() {return name;}
  public void setName(String value) {this.name=value;}
  public String getDescription() {return description;}
  public void setDescription(String value) {this.description=value;}
  public String getLevel() {return level;}
  public void setLevel(String value) {this.level=value;}
  public String getGoal() {return goal;}
  public void setGoal(String value) {this.goal=value;}
  public Boolean getPublished() {return published;}
  public void setPublished(Boolean value) {this.published=value;}
  public UUID getCreatedBy() {return createdBy;}
  public void setCreatedBy(UUID value) {this.createdBy=value;}
  public OffsetDateTime getCreatedAt() {return createdAt;}
  public void setCreatedAt(OffsetDateTime value) {this.createdAt=value;}
  public OffsetDateTime getUpdatedAt() {return updatedAt;}
  public void setUpdatedAt(OffsetDateTime value) {this.updatedAt=value;}

  @Column(name="updated_by") private UUID updatedBy;
  @Column(name="deleted_at") private OffsetDateTime deletedAt;
  @Column(name="deleted_by") private UUID deletedBy;
  public UUID getUpdatedBy() {return updatedBy;}
  public void setUpdatedBy(UUID value) {this.updatedBy=value;}
  public OffsetDateTime getDeletedAt() {return deletedAt;}
  public void setDeletedAt(OffsetDateTime value) {this.deletedAt=value;}
  public UUID getDeletedBy() {return deletedBy;}
  public void setDeletedBy(UUID value) {this.deletedBy=value;}
}
