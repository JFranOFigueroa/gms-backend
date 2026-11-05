package com.gymback.modules.checkin.repositories;
import com.gymback.modules.checkin.interfaces.ICheckinRepository;
import com.gymback.modules.gyms.model.Gym;
import com.gymback.shared.persistence.model.*;
import com.gymback.modules.members.model.GymMember;
import jakarta.persistence.EntityManager;
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
      .map(status->new Access(status,g.getTimezone())).orElse(new Access("not_found","America/Mexico_City"));
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
    List<Map<String,Object>> result=new ArrayList<>();
    for(CheckinEntry e:rows){Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.id);m.put("gym_id",e.gymId);
      m.put("member_id",e.memberId);m.put("guest_pass_id",e.guestPassId);m.put("staff_user_id",e.staffUserId);
      m.put("device_id",e.deviceId);m.put("method",e.method);m.put("allowed",e.allowed);
      m.put("reason_code",e.reasonCode);m.put("reason_detail",e.reasonDetail);m.put("visit_date",e.visitDate);
      m.put("occurred_at",e.occurredAt);m.put("created_at",e.createdAt);m.put("created_by",e.createdBy);
      m.put("updated_at",e.updatedAt);m.put("updated_by",e.updatedBy);m.put("deleted_at",e.deletedAt);
      m.put("deleted_by",e.deletedBy);result.add(m);
    }
    return result;
  }
}
