package com.gymback.modules.checkin.model;
import com.gymback.shared.domain.BaseEntity;
import com.gymback.shared.persistence.model.AuditedRecord;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="checkin_qr_tokens")
public class CheckinQrToken extends AuditedRecord implements BaseEntity {
 @Id public UUID id=UUID.randomUUID();
 @Column(name="gym_id") public UUID gymId;
 @Column(name="device_id") public UUID deviceId;
 @Column(name="token_hash") public String tokenHash;
 @Column(name="issued_at") public OffsetDateTime issuedAt=OffsetDateTime.now();
 @Column(name="expires_at") public OffsetDateTime expiresAt;
 public UUID getId(){return id;}
 public UUID getGymId(){return gymId;}
}
