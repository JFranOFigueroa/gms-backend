package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.util.UUID;
/** Solo lectura, incluye sucursals archivados: la clave CITEXT sigue siendo UNIQUE físicamente. */
@Immutable
@Entity @Table(name="branch")
public class BranchCodeRegistry {
  @Id @Column(name="id") public UUID id;
  @Column(name="code",columnDefinition="citext") public String code;
}
