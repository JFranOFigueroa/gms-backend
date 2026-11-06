package com.gymback.modules.checkin.model;

import com.gymback.shared.domain.BaseEntity;
import com.gymback.shared.persistence.model.AuditedRecord;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Credencial corta, temporal y de un solo uso; nunca se persiste el código en claro. */
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
@Entity @Table(name="checkin_access_codes")
public class CheckinAccessCode extends AuditedRecord implements BaseEntity {
 @Id public UUID id=UUID.randomUUID();
 @Column(name="gym_id",nullable=false) public UUID gymId;
 @Column(name="member_id",nullable=false) public UUID memberId;
 @Column(name="code_hash",nullable=false,length=64) public String codeHash;
 @Column(name="issued_at",nullable=false) public OffsetDateTime issuedAt=OffsetDateTime.now();
 @Column(name="expires_at",nullable=false) public OffsetDateTime expiresAt;
 @Column(name="consumed_at") public OffsetDateTime consumedAt;
 @Column(name="revoked_at") public OffsetDateTime revokedAt;
 public UUID getId(){return id;}
 public UUID getGymId(){return gymId;}
}
