package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
/** Campos transversales de V4. Las filas se archivan, nunca se eliminan físicamente. */
@MappedSuperclass
public abstract class AuditedRecord {
 @Column(name="created_at") public OffsetDateTime createdAt=OffsetDateTime.now();
 @Column(name="created_by") public UUID createdBy;
 @Column(name="updated_at") public OffsetDateTime updatedAt=OffsetDateTime.now();
 @Column(name="updated_by") public UUID updatedBy;
 @Column(name="deleted_at") public OffsetDateTime deletedAt;
 @Column(name="deleted_by") public UUID deletedBy;
}
