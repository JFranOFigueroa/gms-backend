package com.gymback.security;
import com.gymback.security.dto.GymAccess;
import com.gymback.modules.gyms.model.Gym;
import com.gymback.modules.members.model.GymMember;
import com.gymback.shared.persistence.model.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;
@Repository
public class IdentityRepository implements IIdentityRepository {
  private final EntityManager em;
  public IdentityRepository(EntityManager em){this.em=em;}
  private UserCredentials credential(UserAccount u){
    return new UserCredentials(u.id,u.fullName,u.email,u.passwordHash,u.status,u.defaultGymId);
  }
  @Override public Optional<UserCredentials> findCredentialsByEmail(String email){
    return em.createQuery("select u from UserAccount u where lower(u.email)=:email",UserAccount.class)
      .setParameter("email",email.toLowerCase(Locale.ROOT)).getResultStream().findFirst().map(this::credential);
  }
  @Override public Optional<UserCredentials> findCredentialsById(UUID userId){
    return Optional.ofNullable(em.find(UserAccount.class,userId)).map(this::credential);
  }
  @Override public void insertPlatformAdmin(UUID userId){
    PlatformAdminRole role=new PlatformAdminRole();role.userId=userId;em.persist(role);em.flush();
  }
  @Override public boolean isPlatformAdmin(UUID userId){
    return em.find(PlatformAdminRole.class,userId)!=null;
  }
  @Override public boolean isGymActive(UUID gymId){
    Gym gym=em.find(Gym.class,gymId);return gym!=null && "active".equals(gym.getStatus());
  }
  @Override public boolean isModuleEnabled(UUID gymId,String module){
    return em.createQuery("select count(f) from GymFeature f where f.gymId=:gym and f.moduleKey=:module and f.enabled=true",Long.class)
      .setParameter("gym",gymId).setParameter("module",module).getSingleResult()>0;
  }
  @Override public Set<String> activeStaffRoles(UUID userId,UUID gymId){
    return new HashSet<>(em.createQuery("select r.role from GymStaffRole r where r.userId=:user and r.gymId=:gym and r.isActive=true and r.deletedAt is null",String.class)
      .setParameter("user",userId).setParameter("gym",gymId).getResultList());
  }
  @Override public Set<String> activeRoles(UUID userId,UUID gymId){
    Set<String> roles=activeStaffRoles(userId,gymId);
    boolean member=em.createQuery("select count(m) from GymMember m where m.gymId=:gym and m.userId=:user and m.isActive=true",Long.class)
      .setParameter("gym",gymId).setParameter("user",userId).getSingleResult()>0;
    if(member)roles.add("member");
    return roles;
  }
  @Override public List<GymAccess> gymsForUser(UUID userId,boolean platformAdmin){
    List<Gym> gyms=em.createQuery("select g from Gym g where g.status='active' order by g.name,g.id",Gym.class).getResultList();
    if(gyms.isEmpty())return List.of();
    List<Object[]> staff=em.createQuery("select r.gymId,r.role from GymStaffRole r where r.userId=:user and r.isActive=true and r.deletedAt is null",Object[].class)
      .setParameter("user",userId).getResultList();
    List<UUID> memberGyms=em.createQuery("select m.gymId from GymMember m where m.userId=:user and m.isActive=true",UUID.class)
      .setParameter("user",userId).getResultList();
    Map<UUID,Set<String>> memberships=new HashMap<>();
    for(Object[] r:staff) memberships.computeIfAbsent((UUID)r[0],ignored->new TreeSet<>()).add((String)r[1]);
    for(UUID gymId:memberGyms)memberships.computeIfAbsent(gymId,ignored->new TreeSet<>()).add("member");
    List<GymAccess> result=new ArrayList<>();
    for(Gym gym:gyms){
      Set<String> roles=new LinkedHashSet<>();if(platformAdmin)roles.add("admin");
      roles.addAll(memberships.getOrDefault(gym.getId(),Set.of()));
      if(!roles.isEmpty())result.add(new GymAccess(gym.getId(),gym.getName(),gym.getSlug(),List.copyOf(roles)));
    }
    return result;
  }
  @Override public void setDefaultGymIfMissing(UUID userId,UUID gymId){
    UserAccount account=em.find(UserAccount.class,userId);
    if(account!=null && account.defaultGymId==null){account.defaultGymId=gymId;em.flush();}
  }
  @Override public void assignStaff(UUID userId,UUID gymId,String role,UUID assignedBy){
    GymStaffRole.Key key=new GymStaffRole.Key();key.userId=userId;key.gymId=gymId;key.role=role;
    GymStaffRole staff=em.find(GymStaffRole.class,key, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    if(staff==null){staff=new GymStaffRole();staff.gymId=gymId;staff.userId=userId;staff.role=role;
      staff.assignedBy=assignedBy;staff.isActive=true;em.persist(staff);
    }else{staff.deletedAt=null;staff.deletedBy=null;staff.isActive=true;staff.assignedBy=assignedBy;}
    setDefaultGymIfMissing(userId,gymId);em.flush();
  }
  @Override public void deactivateStaff(UUID userId,UUID gymId){
    var roles=em.createQuery("select r from GymStaffRole r where r.gymId=:gym and r.userId=:user and r.isActive=true and r.deletedAt is null",GymStaffRole.class)
      .setParameter("gym",gymId).setParameter("user",userId).getResultList();
    for(GymStaffRole role:roles){role.isActive=false;role.deletedAt=OffsetDateTime.now();}
    em.flush();
  }
  @Override public boolean staffExists(UUID userId,UUID gymId){return !activeStaffRoles(userId,gymId).isEmpty();}
}
