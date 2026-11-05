package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.util.UUID;
/** Vista mínima del nombre del actor, conserva nombres históricos de usuarios archivados. */
@Immutable
@Entity @Table(name="users")
public class AuditActorView {
 @Id @Column(name="id") public UUID id;
 @Column(name="full_name") public String fullName;
}
