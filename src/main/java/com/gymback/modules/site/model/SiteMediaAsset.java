package com.gymback.modules.site.model;

import com.gymback.shared.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Imagen/recurso del sitio (marca), independiente de las sucursales. */
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity
@Table(name = "site_media_assets")
public class SiteMediaAsset implements BaseEntity {
  @Id @Column(name = "id", nullable = false) private UUID id = UUID.randomUUID();
  @Column(name = "name", nullable = false) private String name;
  @Column(name = "media_type", nullable = false) private String mediaType;
  @com.fasterxml.jackson.annotation.JsonIgnore
  @Column(name = "storage_key") private String storageKey;
  @Column(name = "external_url") private String externalUrl;
  @Column(name = "mime_type") private String mimeType;
  @Column(name = "byte_size") private Long byteSize;
  @Column(name = "uploaded_by") private UUID uploadedBy;
  @Column(name = "created_at") private OffsetDateTime createdAt = OffsetDateTime.now();
  @Column(name = "updated_at") private OffsetDateTime updatedAt = OffsetDateTime.now();
  @Column(name = "created_by") private UUID createdBy;
  @Column(name = "updated_by") private UUID updatedBy;
  @Column(name = "deleted_at") private OffsetDateTime deletedAt;
  @Column(name = "deleted_by") private UUID deletedBy;

  @Override public UUID getId() { return id; }
  public void setId(UUID value) { this.id = value; }
  public String getName() { return name; }
  public void setName(String value) { this.name = value; }
  public String getMediaType() { return mediaType; }
  public void setMediaType(String value) { this.mediaType = value; }
  public String getStorageKey() { return storageKey; }
  public void setStorageKey(String value) { this.storageKey = value; }
  public String getExternalUrl() { return externalUrl; }
  public void setExternalUrl(String value) { this.externalUrl = value; }
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

  @Transient public String getUrl() {
    return storageKey == null ? externalUrl : "/api/v1/public/media/" + id;
  }
}
