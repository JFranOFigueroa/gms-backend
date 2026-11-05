package com.gymback.modules.gyms.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.gyms.model.Gym;
import com.gymback.modules.gyms.interfaces.IGymRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.UUID;
@Repository
public class GymRepository extends BaseCRUD<Gym> implements IGymRepository {
  public GymRepository(EntityManager em){super(em,Gym.class,false);}
  @Override public boolean slugExists(String slug,UUID exceptGymId){
    // La restricción unique de CITEXT también cubre los slugs de gimnasios archivados.
    return em.createQuery("select count(g) from GymSlugRegistry g where lower(g.slug)=lower(:slug) and (:except is null or g.id<>:except)",Long.class)
      .setParameter("slug",slug).setParameter("except",exceptGymId).getSingleResult()>0;
  }
}
