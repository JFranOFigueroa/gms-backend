package com.gymback.modules.users.repositories;
import com.gymback.modules.users.interfaces.IUserRepository;
import com.gymback.shared.persistence.model.*;
import com.gymback.modules.members.model.GymMember;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class UserRepository implements IUserRepository {
  private final EntityManager em;
  public UserRepository(EntityManager em){this.em=em;}
  private Map<String,Object> view(UserAccount u){
    Map<String,Object> m=new LinkedHashMap<>();m.put("id",u.id);m.put("fullName",u.fullName);
    m.put("email",u.email);m.put("phone",u.phone);m.put("status",u.status);m.put("createdAt",u.createdAt);return m;
  }
  @Override public Optional<Map<String,Object>> byEmail(String email){
    return em.createQuery("select u from UserAccount u where lower(u.email)=:email",UserAccount.class)
      .setParameter("email",email.toLowerCase(Locale.ROOT)).getResultStream().findFirst().map(this::view);
  }
  @Override public Optional<Map<String,Object>> byId(UUID id){return Optional.ofNullable(em.find(UserAccount.class,id)).map(this::view);}
  @Override public Optional<Map<String,Object>> byEmailInGym(String email,UUID gym){
    String hql="select distinct u from UserAccount u where lower(u.email)=:email and ("+
      "exists (select m.id from GymMember m where m.gymId=:gym and m.userId=u.id) or " +
      "exists (select r.userId from GymStaffRole r where r.gymId=:gym and r.userId=u.id and r.deletedAt is null))";
    return em.createQuery(hql,UserAccount.class).setParameter("gym",gym).setParameter("email",email.toLowerCase(Locale.ROOT))
      .getResultStream().findFirst().map(this::view);
  }
  @Override public Map<String,Object> create(String name,String email,String phone,String hash,String status){
    UserAccount u=new UserAccount();u.fullName=name;u.email=email.toLowerCase(Locale.ROOT);u.phone=phone;u.passwordHash=hash;u.status=status;
    em.persist(u);em.flush();return view(u);
  }
  private String filters(String query,String status){
    return "select u from UserAccount u where 1=1"+
      (query==null || query.isBlank()?"":" and (lower(u.fullName) like :term escape '!' or lower(u.email) like :term escape '!')")+
      (status==null || status.isBlank()?"":" and u.status=:status")+" order by u.fullName,u.id";
  }
  private <T> TypedQuery<T> bind(TypedQuery<T> query,String term,String status){
    if(term!=null && !term.isBlank()){
      String escaped=term.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
      query.setParameter("term","%"+escaped+"%");
    }
    if(status!=null && !status.isBlank())query.setParameter("status",status);
    return query;
  }
  @Override public List<Map<String,Object>> list(String query,String status,int limit,int offset){
    if(limit<1 || limit>100 || offset<0)throw new IllegalArgumentException("Paginación inválida");
    return bind(em.createQuery(filters(query,status),UserAccount.class),query,status).setFirstResult(offset).setMaxResults(limit)
      .getResultStream().map(this::view).toList();
  }
  @Override public long count(String query,String status){
    String hql=filters(query,status).replace("select u from UserAccount u", "select count(u) from UserAccount u").replace(" order by u.fullName,u.id", "");
    return bind(em.createQuery(hql,Long.class),query,status).getSingleResult();
  }
  @Override public Map<String,Object> patch(UUID id,String name,String email,String phone){
    UserAccount u=require(id);if(name!=null)u.fullName=name;if(email!=null)u.email=email.toLowerCase(Locale.ROOT);
    if(phone!=null)u.phone=phone;em.flush();return view(u);
  }
  @Override public Map<String,Object> status(UUID id,String status){UserAccount u=require(id);u.status=status;em.flush();return view(u);}
  private UserAccount require(UUID id){return Optional.ofNullable(em.find(UserAccount.class,id))
    .orElseThrow(()->new com.gymback.shared.exceptions.NotFoundException("Usuario no encontrado"));}
  @Override public boolean isPlatformAdmin(UUID id){return em.find(PlatformAdminRole.class,id)!=null;}
  @Override public void lockAdminStatusChange(){
    // Bloqueo transaccional de TODAS las asignaciones admin en orden estable.
    // Evita que dos transacciones desactiven simultáneamente al último administrador.
    em.createQuery("select p from PlatformAdminRole p order by p.userId",PlatformAdminRole.class)
      .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
  }
  @Override public long activeAdmins(){
    return em.createQuery("select count(p) from PlatformAdminRole p join UserAccount u on u.id=p.userId where u.status='active'",Long.class)
      .getSingleResult();
  }
}
