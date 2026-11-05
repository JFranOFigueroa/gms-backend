package com.gymback.shared.persistence.repositories;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.UUID;
/** Frontera Postgres: el trigger de V4 requiere SET LOCAL en la misma conexión transaccional. */
@Repository
public class AuditActorRepository {
  private final EntityManager em;
  public AuditActorRepository(EntityManager em){this.em=em;}
  public void bind(UUID actor){
    if(actor!=null)em.createNativeQuery("select set_config('app.actor_id', :actor, true)")
      .setParameter("actor",actor.toString()).getSingleResult();
  }
}
