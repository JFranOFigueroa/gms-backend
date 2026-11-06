package com.gymback.modules.checkin.model;
import com.gymback.shared.domain.BaseEntity;
import com.gymback.shared.persistence.model.AuditedRecord;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="checkin_device_sessions")
public class CheckinDeviceSession extends AuditedRecord implements BaseEntity {
 @Id public UUID id=UUID.randomUUID();
 @Column(name="gym_id") public UUID gymId;
 @Column(name="device_id") public UUID deviceId;
 @Column(name="session_token_hash") public String sessionTokenHash;
 @Column(name="expires_at") public OffsetDateTime expiresAt;
 @Column(name="revoked_at") public OffsetDateTime revokedAt;
 @Column(name="last_seen_at") public OffsetDateTime lastSeenAt;
 public UUID getId(){return id;}
 public UUID getGymId(){return gymId;}
}
