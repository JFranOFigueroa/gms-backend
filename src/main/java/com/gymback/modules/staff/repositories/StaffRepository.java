package com.gymback.modules.staff.repositories;
import com.gymback.modules.staff.interfaces.IStaffRepository;
import com.gymback.shared.persistence.model.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class StaffRepository implements IStaffRepository {
  private final EntityManager em;
  public StaffRepository(EntityManager em){this.em=em;}
  private List<Map<String,Object>> search(UUID gym,UUID user){
    String hql="select u,r.role from GymStaffRole r join UserAccount u on u.id=r.userId " +
      "where r.gymId=:gym and r.isActive=true and r.deletedAt is null"+(user==null?"":" and u.id=:user")+
      " order by u.fullName,u.id,r.role";
    var query=em.createQuery(hql,Object[].class).setParameter("gym",gym);
    if(user!=null)query.setParameter("user",user);
    Map<UUID,Map<String,Object>> results=new LinkedHashMap<>();
    for(Object[] row:query.getResultList()){
      UserAccount u=(UserAccount)row[0];
      Map<String,Object> v=results.computeIfAbsent(u.id,ignored->{
        Map<String,Object> m=new LinkedHashMap<>();m.put("id",u.id);m.put("gymId",gym);
        m.put("name",u.fullName);m.put("email",u.email);m.put("phone",u.phone);m.put("status",u.status);
        m.put("roles",new ArrayList<String>());return m;
      });
      @SuppressWarnings("unchecked") List<String> roles=(List<String>)v.get("roles");roles.add((String)row[1]);
    }
    for(var v:results.values()){
      @SuppressWarnings("unchecked")List<String> roles=(List<String>)v.get("roles");
      if(roles.size()==1)v.put("role",roles.get(0));
    }
    return List.copyOf(results.values());
  }
  @Override public List<Map<String,Object>> list(UUID gymId){return search(gymId,null);}
  @Override public Map<String,Object> get(UUID gymId,UUID userId){return search(gymId,userId).stream().findFirst().orElse(null);}
}
