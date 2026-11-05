package com.gymback.modules.members.model;
import com.gymback.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="gym_members")
public class GymMember implements BaseEntity {
  @Id public UUID id=UUID.randomUUID();
  @Column(name="gym_id") public UUID gymId;
  @Column(name="user_id") public UUID userId;
  @Column(name="member_code") public String memberCode;
  @Column(name="birth_date") public LocalDate birthDate;
  public String address;
  @Column(name="emergency_contact_name") public String emergencyContactName;
  @Column(name="emergency_contact_phone") public String emergencyContactPhone;
  @Column(name="photo_asset_id") public UUID photoAssetId;
  public String notes;
  @Column(name="joined_on") public LocalDate joinedOn=LocalDate.now();
  @Column(name="is_active") public Boolean isActive=true;
  @Column(name="created_at") public OffsetDateTime createdAt=OffsetDateTime.now();
  @Column(name="updated_at") public OffsetDateTime updatedAt=OffsetDateTime.now();
  @Override public UUID getId(){return id;}
  @Override public UUID getGymId(){return gymId;}

  @Column(name="created_by") public UUID createdBy;
  @Column(name="updated_by") public UUID updatedBy;
  @Column(name="deleted_at") public OffsetDateTime deletedAt;
  @Column(name="deleted_by") public UUID deletedBy;
  public UUID getCreatedBy() {return createdBy;}
  public void setCreatedBy(UUID value) {this.createdBy=value;}
  public OffsetDateTime getCreatedAt() {return createdAt;}
  public void setCreatedAt(OffsetDateTime value) {this.createdAt=value;}
  public OffsetDateTime getUpdatedAt() {return updatedAt;}
  public void setUpdatedAt(OffsetDateTime value) {this.updatedAt=value;}
  public UUID getUpdatedBy() {return updatedBy;}
  public void setUpdatedBy(UUID value) {this.updatedBy=value;}
  public OffsetDateTime getDeletedAt() {return deletedAt;}
  public void setDeletedAt(OffsetDateTime value) {this.deletedAt=value;}
  public UUID getDeletedBy() {return deletedBy;}
  public void setDeletedBy(UUID value) {this.deletedBy=value;}
}
