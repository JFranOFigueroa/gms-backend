package com.gymback.modules.calendar.repositories;
import com.gymback.modules.calendar.interfaces.IReservationRepository;
import com.gymback.modules.calendar.model.BranchClass;
import com.gymback.modules.branches.model.Branch;
import com.gymback.shared.persistence.model.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.*;
@Repository
public class ReservationRepository implements IReservationRepository {
  private final EntityManager em;
  public ReservationRepository(EntityManager em){this.em=em;}
  @Override public Optional<ClassAvailability> lockClass(UUID branch,UUID id){
    return em.createQuery("select c from BranchClass c where c.branchId=:branch and c.id=:id",BranchClass.class)
      .setParameter("branch",branch).setParameter("id",id).setLockMode(LockModeType.PESSIMISTIC_WRITE)
      .getResultStream().findFirst().map(c->new ClassAvailability(c.getId(),c.getCategory(),c.getCapacity(),c.getStatus(),c.getStartsAt()));
  }
  @Override public long countActive(UUID branch,UUID id){
    return em.createQuery("select count(r) from ClassReservation r where r.branchId=:branch and r.classId=:id and r.status in ('booked','attended')",Long.class)
      .setParameter("branch",branch).setParameter("id",id).getSingleResult();
  }
  @Override public boolean memberHasAccess(UUID branch,UUID user){
    return em.createQuery("select count(a) from MemberAccessView a where a.branchId=:branch and a.userId=:user and a.accessStatus='allowed'",Long.class)
      .setParameter("branch",branch).setParameter("user",user).getSingleResult()>0;
  }
  @Override public boolean duplicate(UUID branch,UUID classId,UUID user){
    return em.createQuery("select count(r) from ClassReservation r where r.branchId=:branch and r.classId=:classId and r.userId=:user and r.status in ('booked','attended')",Long.class)
      .setParameter("branch",branch).setParameter("classId",classId).setParameter("user",user).getSingleResult()>0;
  }
  @Override public boolean multipleNormalAllowed(UUID branch){
    BranchSettings s=em.find(BranchSettings.class,branch);
    return s!=null && Boolean.TRUE.equals(s.allowMultipleNormalClassesPerDay);
  }
  private ZoneId timezone(UUID branch){
    Branch entity=em.find(Branch.class,branch);return ZoneId.of(entity==null?"America/Mexico_City":entity.getTimezone());
  }
  private OffsetDateTime beginning(LocalDate date,ZoneId zone){return date==null?null:date.atStartOfDay(zone).toOffsetDateTime();}
  @Override public boolean bookedNormalSameDay(UUID branch,UUID user,OffsetDateTime start){
    ZoneId zone=timezone(branch);LocalDate day=start.atZoneSameInstant(zone).toLocalDate();
    return em.createQuery("select count(r) from ClassReservation r join BranchClass c on c.branchId=r.branchId and c.id=r.classId "+
      "where r.branchId=:branch and r.userId=:user and r.status in ('booked','attended') and c.category in ('normal','functional') "+
      "and c.startsAt>=:from and c.startsAt<:to",Long.class)
      .setParameter("branch",branch).setParameter("user",user).setParameter("from",beginning(day,zone))
      .setParameter("to",beginning(day.plusDays(1),zone)).getSingleResult()>0;
  }
  @Override public UUID book(UUID branch,UUID classId,UUID user){
    ClassReservation r=new ClassReservation();r.branchId=branch;r.classId=classId;r.userId=user;
    em.persist(r);em.flush();return r.id;
  }
  @Override public boolean cancelOwn(UUID branch,UUID reservation,UUID user){
    var rows=em.createQuery("select r from ClassReservation r where r.branchId=:branch and r.id=:id and r.userId=:user and r.status='booked'",ClassReservation.class)
      .setParameter("branch",branch).setParameter("id",reservation).setParameter("user",user).getResultList();
    if(rows.isEmpty())return false;
    ClassReservation r=rows.get(0);r.status="cancelled";r.cancelledAt=OffsetDateTime.now();em.flush();return true;
  }
  private String ownFilter(LocalDate from,LocalDate to,String status){
    return "select r,c from ClassReservation r join BranchClass c on c.branchId=r.branchId and c.id=r.classId " +
      "where r.branchId=:branch and r.userId=:user"+(from==null?"":" and c.startsAt>=:from")+
      (to==null?"":" and c.startsAt<:to")+(status==null?"":" and r.status=:status");
  }
  private TypedQuery<Object[]> bindOwn(TypedQuery<Object[]> query,UUID branch,UUID user,LocalDate from,LocalDate to,String status){
    ZoneId zone=timezone(branch);
    query.setParameter("branch",branch).setParameter("user",user);
    if(from!=null)query.setParameter("from",beginning(from,zone));
    if(to!=null)query.setParameter("to",beginning(to.plusDays(1),zone));
    if(status!=null)query.setParameter("status",status);
    return query;
  }
  @Override public List<Map<String,Object>> own(UUID branch,UUID user,LocalDate from,LocalDate to,String status,int offset,int limit){
    var rows=bindOwn(em.createQuery(ownFilter(from,to,status)+" order by c.startsAt,r.id",Object[].class),branch,user,from,to,status)
      .setFirstResult(offset).setMaxResults(limit).getResultList();
    List<Map<String,Object>> result=new ArrayList<>();
    for(Object[] row:rows){ClassReservation r=(ClassReservation)row[0];BranchClass c=(BranchClass)row[1];
      Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.id);m.put("classId",r.classId);m.put("status",r.status);
      m.put("className",c.getName());m.put("classStartsAt",c.getStartsAt());m.put("classEndsAt",c.getEndsAt());result.add(m);}
    return result;
  }
  @Override public long ownCount(UUID branch,UUID user,LocalDate from,LocalDate to,String status){
    // Cuenta la misma relación filtrada, sin paginación.
    String hql=ownFilter(from,to,status).replace("select r,c from ","select count(r) from ");
    var q=em.createQuery(hql,Long.class);ZoneId zone=timezone(branch);
    q.setParameter("branch",branch).setParameter("user",user);
    if(from!=null)q.setParameter("from",beginning(from,zone));
    if(to!=null)q.setParameter("to",beginning(to.plusDays(1),zone));
    if(status!=null)q.setParameter("status",status);
    return q.getSingleResult();
  }
  @Override public Optional<Map<String,Object>> availability(UUID branch,UUID classId,UUID user){
    var row=em.createQuery("select c from BranchClass c where c.branchId=:branch and c.id=:id",BranchClass.class)
      .setParameter("branch",branch).setParameter("id",classId).getResultStream().findFirst();
    if(row.isEmpty())return Optional.empty();
    BranchClass c=row.get();long count=countActive(branch,classId);
    String own=em.createQuery("select r.status from ClassReservation r where r.branchId=:branch and r.classId=:id and r.userId=:user and r.status in ('booked','attended')",String.class)
      .setParameter("branch",branch).setParameter("id",classId).setParameter("user",user)
      .setMaxResults(1).getResultStream().findFirst().orElse(null);
    int cap=c.getCapacity();Map<String,Object> m=new LinkedHashMap<>();
    m.put("classId",classId);m.put("capacity",cap);m.put("reserved",count);
    m.put("available",Math.max(0,cap-count));m.put("myReservationStatus",own);
    m.put("bookable","scheduled".equals(c.getStatus()) && c.getStartsAt().isAfter(OffsetDateTime.now()) && count<cap && own==null);
    return Optional.of(m);
  }
  @Override public List<Map<String,Object>> listForClass(UUID branch,UUID classId){
    var rows=em.createQuery("select r from ClassReservation r where r.branchId=:branch and r.classId=:id order by r.bookedAt desc",ClassReservation.class)
      .setParameter("branch",branch).setParameter("id",classId).setMaxResults(100).getResultList();
    List<Map<String,Object>> result=new ArrayList<>();
    for(ClassReservation r:rows){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.id);m.put("class_id",r.classId);
      m.put("user_id",r.userId);m.put("status",r.status);m.put("booked_at",r.bookedAt);result.add(m);}
    return result;
  }
}
