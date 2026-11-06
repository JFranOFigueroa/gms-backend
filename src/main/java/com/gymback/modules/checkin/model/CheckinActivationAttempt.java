package com.gymback.modules.checkin.model;
import com.gymback.shared.persistence.model.AuditedRecord;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="checkin_activation_attempts")
public class CheckinActivationAttempt extends AuditedRecord {
 @Id public UUID id=UUID.randomUUID();
 @Column(name="client_hash") public String clientHash;
 @Column(name="tried_at") public OffsetDateTime triedAt=OffsetDateTime.now();
}
