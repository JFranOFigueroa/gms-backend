package com.gymback.modules.checkin.model;
import com.gymback.shared.domain.BaseEntity;
import com.gymback.shared.persistence.model.AuditedRecord;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="checkin_devices")
public class CheckinDevice extends AuditedRecord implements BaseEntity {
 @Id public UUID id=UUID.randomUUID();
 @Column(name="gym_id") public UUID gymId;
 @Column(name="device_name") public String deviceName;
 @Column(name="device_secret_hash") public String deviceSecretHash;
 @Column(name="is_active") public Boolean active=true;
 @Column(name="last_seen_at") public OffsetDateTime lastSeenAt;
 @Column(name="activation_expires_at") public OffsetDateTime activationExpiresAt;
 @Column(name="activated_at") public OffsetDateTime activatedAt;
 public UUID getId(){return id;}
 public UUID getGymId(){return gymId;}
}
