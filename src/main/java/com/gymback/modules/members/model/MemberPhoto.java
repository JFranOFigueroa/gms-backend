package com.gymback.modules.members.model;

import com.gymback.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Fotografía privada del expediente. Vive fuera de site_media_assets. */
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "member_photos")
public class MemberPhoto implements BaseEntity {
  @Id @Column(name = "id", nullable = false) private UUID id = UUID.randomUUID();
  @Column(name = "branch_id", nullable = false) private UUID branchId;
  @Column(name = "member_id", nullable = false) private UUID memberId;
  @Column(name = "storage_key", nullable = false) private String storageKey;
  @Column(name = "mime_type", nullable = false) private String mimeType;
  @Column(name = "byte_size", nullable = false) private Long byteSize;
  @Column(name = "uploaded_by") private UUID uploadedBy;
  @Column(name = "created_at") private OffsetDateTime createdAt = OffsetDateTime.now();
  @Column(name = "updated_at") private OffsetDateTime updatedAt = OffsetDateTime.now();
  @Column(name = "created_by") private UUID createdBy;
  @Column(name = "updated_by") private UUID updatedBy;
  @Column(name = "deleted_at") private OffsetDateTime deletedAt;
  @Column(name = "deleted_by") private UUID deletedBy;

  @Override public UUID getId() { return id; }
  @Override public UUID getBranchId() { return branchId; }
  public void setBranchId(UUID value) { this.branchId = value; }
  public UUID getMemberId() { return memberId; }
  public void setMemberId(UUID value) { this.memberId = value; }
  public String getStorageKey() { return storageKey; }
  public void setStorageKey(String value) { this.storageKey = value; }
  public String getMimeType() { return mimeType; }
  public void setMimeType(String value) { this.mimeType = value; }
  public Long getByteSize() { return byteSize; }
  public void setByteSize(Long value) { this.byteSize = value; }
  public UUID getUploadedBy() { return uploadedBy; }
  public void setUploadedBy(UUID value) { this.uploadedBy = value; }
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
