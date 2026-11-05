package com.gymback.shared.persistence.model;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import java.util.UUID;
/** Solo lectura, incluye gimnasios archivados: la clave CITEXT sigue siendo UNIQUE físicamente. */
@Immutable
@Entity @Table(name="gym")
public class GymSlugRegistry {
  @Id @Column(name="id") public UUID id;
  @Column(name="slug",columnDefinition="citext") public String slug;
}
