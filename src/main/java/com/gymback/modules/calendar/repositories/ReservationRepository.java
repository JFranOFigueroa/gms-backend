package com.gymback.modules.calendar.repositories;
import com.gymback.modules.calendar.interfaces.IReservationRepository;
import com.gymback.modules.calendar.model.GymClass;
import com.gymback.modules.gyms.model.Gym;
import com.gymback.shared.persistence.model.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.*;
@Repository
public class ReservationRepository implements IReservationRepository {
  private final EntityManager em;
  public ReservationRepository(EntityManager em){this.em=em;}
  @Override public Optional<ClassAvailability> lockClass(UUID gym,UUID id){
    return em.createQuery("select c from GymClass c where c.gymId=:gym and c.id=:id",GymClass.class)
      .setParameter("gym",gym).setParameter("id",id).setLockMode(LockModeType.PESSIMISTIC_WRITE)
      .getResultStream().findFirst().map(c->new ClassAvailability(c.getId(),c.getCategory(),c.getCapacity(),c.getStatus(),c.getStartsAt()));
  }
  @Override public long countActive(UUID gym,UUID id){
    return em.createQuery("select count(r) from ClassReservation r where r.gymId=:gym and r.classId=:id and r.status in ('booked','attended')",Long.class)
      .setParameter("gym",gym).setParameter("id",id).getSingleResult();
  }
  @Override public boolean memberHasAccess(UUID gym,UUID user){
    return em.createQuery("select count(a) from MemberAccessView a where a.gymId=:gym and a.userId=:user and a.accessStatus='allowed'",Long.class)
      .setParameter("gym",gym).setParameter("user",user).getSingleResult()>0;
  }
  @Override public boolean duplicate(UUID gym,UUID classId,UUID user){
    return em.createQuery("select count(r) from ClassReservation r where r.gymId=:gym and r.classId=:classId and r.userId=:user and r.status in ('booked','attended')",Long.class)
      .setParameter("gym",gym).setParameter("classId",classId).setParameter("user",user).getSingleResult()>0;
  }
  @Override public boolean multipleNormalAllowed(UUID gym){
    GymConfiguration s=em.find(GymConfiguration.class,gym);
    return s!=null && Boolean.TRUE.equals(s.allowMultipleNormalClassesPerDay);
  }
  private ZoneId timezone(UUID gym){
    Gym entity=em.find(Gym.class,gym);return ZoneId.of(entity==null?"America/Mexico_City":entity.getTimezone());
  }
  private OffsetDateTime beginning(LocalDate date,ZoneId zone){return date==null?null:date.atStartOfDay(zone).toOffsetDateTime();}
  @Override public boolean bookedNormalSameDay(UUID gym,UUID user,OffsetDateTime start){
    ZoneId zone=timezone(gym);LocalDate day=start.atZoneSameInstant(zone).toLocalDate();
    return em.createQuery("select count(r) from ClassReservation r join GymClass c on c.gymId=r.gymId and c.id=r.classId "+
      "where r.gymId=:gym and r.userId=:user and r.status in ('booked','attended') and c.category in ('normal','functional') "+
      "and c.startsAt>=:from and c.startsAt<:to",Long.class)
      .setParameter("gym",gym).setParameter("user",user).setParameter("from",beginning(day,zone))
      .setParameter("to",beginning(day.plusDays(1),zone)).getSingleResult()>0;
  }
  @Override public UUID book(UUID gym,UUID classId,UUID user){
    ClassReservation r=new ClassReservation();r.gymId=gym;r.classId=classId;r.userId=user;
    em.persist(r);em.flush();return r.id;
  }
  @Override public boolean cancelOwn(UUID gym,UUID reservation,UUID user){
    var rows=em.createQuery("select r from ClassReservation r where r.gymId=:gym and r.id=:id and r.userId=:user and r.status='booked'",ClassReservation.class)
      .setParameter("gym",gym).setParameter("id",reservation).setParameter("user",user).getResultList();
    if(rows.isEmpty())return false;
    ClassReservation r=rows.get(0);r.status="cancelled";r.cancelledAt=OffsetDateTime.now();em.flush();return true;
  }
  private String ownFilter(LocalDate from,LocalDate to,String status){
    return "select r,c from ClassReservation r join GymClass c on c.gymId=r.gymId and c.id=r.classId " +
      "where r.gymId=:gym and r.userId=:user"+(from==null?"":" and c.startsAt>=:from")+
      (to==null?"":" and c.startsAt<:to")+(status==null?"":" and r.status=:status");
  }
  private TypedQuery<Object[]> bindOwn(TypedQuery<Object[]> query,UUID gym,UUID user,LocalDate from,LocalDate to,String status){
    ZoneId zone=timezone(gym);
    query.setParameter("gym",gym).setParameter("user",user);
    if(from!=null)query.setParameter("from",beginning(from,zone));
    if(to!=null)query.setParameter("to",beginning(to.plusDays(1),zone));
    if(status!=null)query.setParameter("status",status);
    return query;
  }
  @Override public List<Map<String,Object>> own(UUID gym,UUID user,LocalDate from,LocalDate to,String status,int offset,int limit){
    var rows=bindOwn(em.createQuery(ownFilter(from,to,status)+" order by c.startsAt,r.id",Object[].class),gym,user,from,to,status)
      .setFirstResult(offset).setMaxResults(limit).getResultList();
    List<Map<String,Object>> result=new ArrayList<>();
    for(Object[] row:rows){ClassReservation r=(ClassReservation)row[0];GymClass c=(GymClass)row[1];
      Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.id);m.put("classId",r.classId);m.put("status",r.status);
      m.put("className",c.getName());m.put("classStartsAt",c.getStartsAt());m.put("classEndsAt",c.getEndsAt());result.add(m);}
    return result;
  }
  @Override public long ownCount(UUID gym,UUID user,LocalDate from,LocalDate to,String status){
    // Cuenta la misma relación filtrada, sin paginación.
    String hql=ownFilter(from,to,status).replace("select r,c from ","select count(r) from ");
    var q=em.createQuery(hql,Long.class);ZoneId zone=timezone(gym);
    q.setParameter("gym",gym).setParameter("user",user);
    if(from!=null)q.setParameter("from",beginning(from,zone));
    if(to!=null)q.setParameter("to",beginning(to.plusDays(1),zone));
    if(status!=null)q.setParameter("status",status);
    return q.getSingleResult();
  }
  @Override public Optional<Map<String,Object>> availability(UUID gym,UUID classId,UUID user){
    var row=em.createQuery("select c from GymClass c where c.gymId=:gym and c.id=:id",GymClass.class)
      .setParameter("gym",gym).setParameter("id",classId).getResultStream().findFirst();
    if(row.isEmpty())return Optional.empty();
    GymClass c=row.get();long count=countActive(gym,classId);
    String own=em.createQuery("select r.status from ClassReservation r where r.gymId=:gym and r.classId=:id and r.userId=:user and r.status in ('booked','attended')",String.class)
      .setParameter("gym",gym).setParameter("id",classId).setParameter("user",user)
      .setMaxResults(1).getResultStream().findFirst().orElse(null);
    int cap=c.getCapacity();Map<String,Object> m=new LinkedHashMap<>();
    m.put("classId",classId);m.put("capacity",cap);m.put("reserved",count);
    m.put("available",Math.max(0,cap-count));m.put("myReservationStatus",own);
    m.put("bookable","scheduled".equals(c.getStatus()) && c.getStartsAt().isAfter(OffsetDateTime.now()) && count<cap && own==null);
    return Optional.of(m);
  }
  @Override public List<Map<String,Object>> listForClass(UUID gym,UUID classId){
    var rows=em.createQuery("select r from ClassReservation r where r.gymId=:gym and r.classId=:id order by r.bookedAt desc",ClassReservation.class)
      .setParameter("gym",gym).setParameter("id",classId).setMaxResults(100).getResultList();
    List<Map<String,Object>> result=new ArrayList<>();
    for(ClassReservation r:rows){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.id);m.put("class_id",r.classId);
      m.put("user_id",r.userId);m.put("status",r.status);m.put("booked_at",r.bookedAt);result.add(m);}
    return result;
  }
}
