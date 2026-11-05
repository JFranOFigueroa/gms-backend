package com.gymback.modules.calendar.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.calendar.model.GymClass;
import com.gymback.modules.calendar.interfaces.IGymClassRepository;
import com.gymback.modules.gyms.model.Gym;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class GymClassRepository extends BaseCRUD<GymClass> implements IGymClassRepository {
  public GymClassRepository(EntityManager em){super(em,GymClass.class,true);}
  @Override public List<GymClass> calendar(UUID gymId,LocalDate from,LocalDate to,String status,int offset,int limit){
    if(gymId==null || offset<0 || limit<1 || limit>100)throw new IllegalArgumentException("Paginación inválida");
    Gym gym=em.find(Gym.class,gymId);
    if(gym==null)return List.of();
    ZoneId zone=ZoneId.of(gym.getTimezone());
    OffsetDateTime start=from==null?null:from.atStartOfDay(zone).toOffsetDateTime();
    OffsetDateTime end=to==null?null:to.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
    String jpql="select c from GymClass c where c.gymId=:gym"+
      (start==null?"":" and c.startsAt>=:start")+(end==null?"":" and c.startsAt<:end")+
      (status==null || "all".equals(status)?"":" and c.status=:status")+" order by c.startsAt,c.id";
    var query=em.createQuery(jpql,GymClass.class).setParameter("gym",gymId).setFirstResult(offset).setMaxResults(limit);
    if(start!=null)query.setParameter("start",start);
    if(end!=null)query.setParameter("end",end);
    if(status!=null && !"all".equals(status))query.setParameter("status",status);
    return query.getResultList();
  }
}
