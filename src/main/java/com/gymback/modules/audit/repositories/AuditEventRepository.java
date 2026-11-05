package com.gymback.modules.audit.repositories;
import com.gymback.modules.audit.interfaces.IAuditEventRepository;
import com.gymback.shared.persistence.model.AuditEventRecord;
import com.gymback.shared.persistence.model.UserAccount;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class AuditEventRepository implements IAuditEventRepository {
  private final EntityManager em;
  public AuditEventRepository(EntityManager em){this.em=em;}
  private List<Map<String,Object>> search(UUID gym,int offset,int limit,boolean all){
    if(offset<0 || limit<1 || limit>100 || (!all && gym==null))throw new IllegalArgumentException("Paginación inválida");
    String hql="select e,u.fullName from AuditEventRecord e left join AuditActorView u on u.id=e.actorUserId"+
      (gym==null?"":" where e.gymId=:gym")+" order by e.occurredAt desc,e.id desc";
    var q=em.createQuery(hql,Object[].class).setFirstResult(offset).setMaxResults(limit);
    if(gym!=null)q.setParameter("gym",gym);
    List<Map<String,Object>> result=new ArrayList<>();
    for(Object[] row:q.getResultList()){
      AuditEventRecord e=(AuditEventRecord)row[0];Map<String,Object> m=new LinkedHashMap<>();
      m.put("id",e.id);m.put("gymId",e.gymId);m.put("actorId",e.actorUserId);m.put("actorName",row[1]);
      m.put("action",e.action);m.put("entityType",e.entityType);m.put("entityId",e.entityId);
      m.put("entityKey",e.entityKey);m.put("occurredAt",e.occurredAt);result.add(m);
    }
    return result;
  }
  @Override public List<Map<String,Object>> list(UUID gym,int offset,int limit){return search(gym,offset,limit,false);}
  @Override public List<Map<String,Object>> all(UUID gym,int offset,int limit){return search(gym,offset,limit,true);}
}
