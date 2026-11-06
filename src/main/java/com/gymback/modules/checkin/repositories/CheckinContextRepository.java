package com.gymback.modules.checkin.repositories;
import com.gymback.modules.checkin.interfaces.ICheckinContextRepository;
import com.gymback.modules.gyms.model.Gym;
import com.gymback.shared.persistence.model.GymConfiguration;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class CheckinContextRepository implements ICheckinContextRepository {
 private final EntityManager em;
 public CheckinContextRepository(EntityManager em){this.em=em;}
 public Optional<GymContext> bySlug(String slug){
  if(slug==null||slug.isBlank()||slug.length()>150)return Optional.empty();
  return em.createQuery("select g from Gym g where lower(g.slug)=lower(:slug) and g.status='active'",Gym.class)
   .setParameter("slug",slug.trim()).setMaxResults(1).getResultStream().findFirst().map(this::context);
 }
 public Optional<GymContext> byId(UUID gym){return Optional.ofNullable(em.find(Gym.class,gym)).filter(g->"active".equals(g.getStatus())).map(this::context);}
 private GymContext context(Gym gym){
  GymConfiguration cfg=em.find(GymConfiguration.class,gym.getId());
  int ttl=cfg==null||cfg.checkinQrTtlSeconds==null?10:cfg.checkinQrTtlSeconds;
  return new GymContext(gym.getId(),gym.getName(),gym.getSlug(),gym.getTimezone(),Math.max(5,Math.min(300,ttl)));
 }
}
