package com.gymback.modules.checkin.repositories;
import com.gymback.modules.checkin.interfaces.ICheckinRepository;
import com.gymback.modules.gyms.model.Gym;
import com.gymback.shared.persistence.model.*;
import com.gymback.modules.members.model.GymMember;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.*;
@Repository
public class CheckinRepository implements ICheckinRepository {
  private final EntityManager em;
  public CheckinRepository(EntityManager em){this.em=em;}
  @Override public Access evaluate(UUID gym,UUID member){
    Gym g=em.find(Gym.class,gym);
    if(g==null)return new Access("not_found","America/Mexico_City");
    return em.createQuery("select a.accessStatus from MemberAccessView a where a.gymId=:gym and a.memberId=:member",String.class)
      .setParameter("gym",gym).setParameter("member",member).getResultStream().findFirst()
      .map(status->new Access(status,g.getTimezone())).orElse(new Access("not_found",g.getTimezone()));
  }
  @Override public boolean alreadyAllowed(UUID gym,UUID member,LocalDate date){
    return em.createQuery("select count(c) from CheckinEntry c where c.gymId=:gym and c.memberId=:member and c.visitDate=:date and c.allowed=true",Long.class)
      .setParameter("gym",gym).setParameter("member",member).setParameter("date",date).getSingleResult()>0;
  }
  @Override public UUID record(UUID gym,UUID member,LocalDate date,boolean allowed,String reason,UUID actor){
    CheckinEntry e=new CheckinEntry();e.gymId=gym;e.memberId=member;e.visitDate=date;
    e.method="manual";e.allowed=allowed;e.reasonCode=reason;e.createdBy=actor;
    em.persist(e);em.flush();return e.id;
  }
  @Override public List<Map<String,Object>> own(UUID gym,UUID user,LocalDate from,LocalDate to,int limit){
    String hql="select e from CheckinEntry e join GymMember m on m.gymId=e.gymId and m.id=e.memberId "+
      "where e.gymId=:gym and m.userId=:user"+(from==null?"":" and e.visitDate>=:from")+
      (to==null?"":" and e.visitDate<=:to")+" order by e.occurredAt desc,e.id desc";
    var q=em.createQuery(hql,CheckinEntry.class).setParameter("gym",gym).setParameter("user",user).setMaxResults(limit);
    if(from!=null)q.setParameter("from",from);if(to!=null)q.setParameter("to",to);
    return q.getResultStream().map(e->{Map<String,Object> m=new LinkedHashMap<>();m.put("eventId",e.id);m.put("occurredAt",e.occurredAt);
      m.put("visitDate",e.visitDate);m.put("allowed",e.allowed);m.put("method",e.method);m.put("reason",e.reasonCode);return m;}).toList();
  }
  @Override public List<Map<String,Object>> list(UUID gym,int limit){
    if(limit<1||limit>100)throw new IllegalArgumentException("limit 1..100");
    var rows=em.createQuery("select e from CheckinEntry e where e.gymId=:gym order by e.occurredAt desc",CheckinEntry.class)
      .setParameter("gym",gym).setMaxResults(limit).getResultList();
    // Resolver nombres en lote (sin N+1). Mantener todas las claves JSON antiguas.
    Set<UUID> memberIds=new HashSet<>(),userIds=new HashSet<>(),deviceIds=new HashSet<>();
    for(CheckinEntry e:rows){
      if(e.memberId!=null)memberIds.add(e.memberId);
      if(e.staffUserId!=null)userIds.add(e.staffUserId);
      if(e.createdBy!=null)userIds.add(e.createdBy);
      if(e.deviceId!=null)deviceIds.add(e.deviceId);
    }
    Map<UUID,String> memberNames=new HashMap<>(),userNames=new HashMap<>(),deviceNames=new HashMap<>();
    if(!memberIds.isEmpty()){
      var named=em.createQuery("select m.id,u.fullName from GymMember m join UserAccount u on u.id=m.userId " +
        "where m.gymId=:gym and m.id in :ids",Object[].class)
        .setParameter("gym",gym).setParameter("ids",memberIds).getResultList();
      for(Object[] row:named)memberNames.put((UUID)row[0],(String)row[1]);
    }
    if(!userIds.isEmpty()){
      var named=em.createQuery("select u.id,u.fullName from UserAccount u where u.id in :ids",Object[].class)
        .setParameter("ids",userIds).getResultList();
      for(Object[] row:named)userNames.put((UUID)row[0],(String)row[1]);
    }
    if(!deviceIds.isEmpty()){
      var named=em.createQuery("select d.id,d.deviceName from CheckinDevice d where d.gymId=:gym and d.id in :ids",Object[].class)
        .setParameter("gym",gym).setParameter("ids",deviceIds).getResultList();
      for(Object[] row:named)deviceNames.put((UUID)row[0],(String)row[1]);
    }
    List<Map<String,Object>> result=new ArrayList<>();
    for(CheckinEntry e:rows){Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.id);m.put("gym_id",e.gymId);
      m.put("member_id",e.memberId);m.put("guest_pass_id",e.guestPassId);m.put("staff_user_id",e.staffUserId);
      m.put("device_id",e.deviceId);m.put("method",e.method);m.put("allowed",e.allowed);
      m.put("reason_code",e.reasonCode);m.put("reason_detail",e.reasonDetail);m.put("visit_date",e.visitDate);
      m.put("occurred_at",e.occurredAt);m.put("created_at",e.createdAt);m.put("created_by",e.createdBy);
      m.put("updated_at",e.updatedAt);m.put("updated_by",e.updatedBy);m.put("deleted_at",e.deletedAt);
      m.put("deleted_by",e.deletedBy);
      // Campos aditivos, exclusivamente en la consulta privada por gimnasio.
      String memberName=memberNames.get(e.memberId),staffName=userNames.get(e.staffUserId);
      m.put("member_name",memberName);m.put("staff_name",staffName);
      m.put("recorded_by_name",userNames.get(e.createdBy));
      m.put("device_name",deviceNames.get(e.deviceId));
      m.put("display_name",memberName!=null?memberName:staffName!=null?staffName:
        e.memberId!=null?"Miembro":e.staffUserId!=null?"Personal":e.guestPassId!=null?"Invitado":"Acceso sin identificar");
      result.add(m);
    }
    return result;
  }

  @Override public Optional<UUID> memberIdForUser(UUID gym,UUID user){
    return em.createQuery("select m.id from GymMember m where m.gymId=:gym and m.userId=:user and m.isActive=true",UUID.class)
      .setParameter("gym",gym).setParameter("user",user).setMaxResults(1).getResultStream().findFirst();
  }
  @Override public boolean lockMember(UUID gym,UUID member){
    GymMember m=em.find(GymMember.class,member,LockModeType.PESSIMISTIC_WRITE);
    return m!=null && gym.equals(m.gymId);
  }
  @Override public void lockDevice(UUID gym,UUID device){
    var d=em.find(com.gymback.modules.checkin.model.CheckinDevice.class,device,LockModeType.PESSIMISTIC_WRITE);
    if(d==null || !gym.equals(d.gymId) || !Boolean.TRUE.equals(d.active) || d.activatedAt==null)
      throw new org.springframework.security.access.AccessDeniedException("Dispositivo no autorizado");
  }
  @Override public Optional<Attempt> priorAttempt(UUID gym,String hash){
    return em.createQuery("select e from CheckinEntry e where e.gymId=:gym and e.idempotencyKeyHash=:hash",CheckinEntry.class)
      .setParameter("gym",gym).setParameter("hash",hash).setMaxResults(1).getResultStream()
      .map(e->new Attempt(Boolean.TRUE.equals(e.allowed),e.reasonCode,e.visitDate,e.occurredAt,e.requestFingerprint)).findFirst();
  }
  @Override public Attempt recordQr(UUID gym,UUID member,UUID device,String method,LocalDate date,boolean allowed,
                                   String reason,UUID actor,String keyHash,String fingerprint){
    CheckinEntry e=new CheckinEntry();e.gymId=gym;e.memberId=member;e.deviceId=device;e.method=method;
    e.visitDate=date;e.allowed=allowed;e.reasonCode=reason;e.createdBy=actor;
    e.idempotencyKeyHash=keyHash;e.requestFingerprint=fingerprint;
    em.persist(e);em.flush();return new Attempt(allowed,reason,date,e.occurredAt,fingerprint);
  }
  @Override public long countRecent(UUID gym,UUID member,UUID device,OffsetDateTime since){
    String hql="select count(e) from CheckinEntry e where e.gymId=:gym and e.occurredAt>=:since"+
      (member!=null?" and e.memberId=:member":"")+(device!=null?" and e.deviceId=:device":"");
    var q=em.createQuery(hql,Long.class).setParameter("gym",gym).setParameter("since",since);
    if(member!=null)q.setParameter("member",member);if(device!=null)q.setParameter("device",device);
    return q.getSingleResult();
  }
  @Override public long countRecentCodeAttempts(UUID gym,UUID device,OffsetDateTime since){
    return em.createQuery("select count(e) from CheckinEntry e where e.gymId=:gym and e.deviceId=:device and e.method='kiosk_code' and e.occurredAt>=:since",Long.class)
      .setParameter("gym",gym).setParameter("device",device).setParameter("since",since).getSingleResult();
  }
  @Override public List<EventPoint> eventsForStats(UUID gym,LocalDate from,LocalDate to){
    var query=em.createQuery("select e from CheckinEntry e where e.gymId=:gym and e.visitDate>=:from and e.visitDate<=:to",CheckinEntry.class)
      .setParameter("gym",gym).setParameter("from",from).setParameter("to",to);
    return query.getResultStream().map(e->new EventPoint(e.occurredAt,Boolean.TRUE.equals(e.allowed))).toList();
  }
}