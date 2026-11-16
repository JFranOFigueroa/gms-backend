package com.gymback.security;
import com.gymback.security.dto.BranchAccess;
import com.gymback.modules.branches.model.Branch;
import com.gymback.modules.members.model.BranchMember;
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
    return new UserCredentials(u.id,u.fullName,u.email,u.passwordHash,u.status,u.defaultBranchId);
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
  @Override public boolean isBranchActive(UUID branchId){
    Branch branch=em.find(Branch.class,branchId);return branch!=null && "active".equals(branch.getStatus());
  }
  @Override public boolean isModuleEnabled(UUID branchId,String module){
    return em.createQuery("select count(f) from BranchModule f where f.branchId=:branch and f.moduleKey=:module and f.enabled=true",Long.class)
      .setParameter("branch",branchId).setParameter("module",module).getSingleResult()>0;
  }
  @Override public Set<String> activeStaffRoles(UUID userId,UUID branchId){
    return new HashSet<>(em.createQuery("select r.role from BranchStaffRole r where r.userId=:user and r.branchId=:branch and r.isActive=true and r.deletedAt is null",String.class)
      .setParameter("user",userId).setParameter("branch",branchId).getResultList());
  }
  @Override public Set<String> activeRoles(UUID userId,UUID branchId){
    Set<String> roles=activeStaffRoles(userId,branchId);
    boolean member=em.createQuery("select count(m) from BranchMember m where m.branchId=:branch and m.userId=:user and m.isActive=true",Long.class)
      .setParameter("branch",branchId).setParameter("user",userId).getSingleResult()>0;
    if(member)roles.add("member");
    return roles;
  }
  @Override public List<BranchAccess> branchesForUser(UUID userId,boolean platformAdmin){
    List<Branch> branches=em.createQuery("select g from Branch g where g.status='active' order by g.name,g.id",Branch.class).getResultList();
    if(branches.isEmpty())return List.of();
    List<Object[]> staff=em.createQuery("select r.branchId,r.role from BranchStaffRole r where r.userId=:user and r.isActive=true and r.deletedAt is null",Object[].class)
      .setParameter("user",userId).getResultList();
    List<UUID> memberBranches=em.createQuery("select m.branchId from BranchMember m where m.userId=:user and m.isActive=true",UUID.class)
      .setParameter("user",userId).getResultList();
    Map<UUID,Set<String>> memberships=new HashMap<>();
    for(Object[] r:staff) memberships.computeIfAbsent((UUID)r[0],ignored->new TreeSet<>()).add((String)r[1]);
    for(UUID branchId:memberBranches)memberships.computeIfAbsent(branchId,ignored->new TreeSet<>()).add("member");
    List<BranchAccess> result=new ArrayList<>();
    for(Branch branch:branches){
      Set<String> roles=new LinkedHashSet<>();if(platformAdmin)roles.add("admin");
      roles.addAll(memberships.getOrDefault(branch.getId(),Set.of()));
      if(!roles.isEmpty())result.add(new BranchAccess(branch.getId(),branch.getName(),branch.getCode(),List.copyOf(roles)));
    }
    return result;
  }
  @Override public void setDefaultBranchIfMissing(UUID userId,UUID branchId){
    UserAccount account=em.find(UserAccount.class,userId);
    if(account!=null && account.defaultBranchId==null){account.defaultBranchId=branchId;em.flush();}
  }
  @Override public void assignStaff(UUID userId,UUID branchId,String role,UUID assignedBy){
    BranchStaffRole.Key key=new BranchStaffRole.Key();key.userId=userId;key.branchId=branchId;key.role=role;
    BranchStaffRole staff=em.find(BranchStaffRole.class,key, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
    if(staff==null){staff=new BranchStaffRole();staff.branchId=branchId;staff.userId=userId;staff.role=role;
      staff.assignedBy=assignedBy;staff.isActive=true;em.persist(staff);
    }else{staff.deletedAt=null;staff.deletedBy=null;staff.isActive=true;staff.assignedBy=assignedBy;}
    setDefaultBranchIfMissing(userId,branchId);em.flush();
  }
  @Override public void deactivateStaff(UUID userId,UUID branchId){
    var roles=em.createQuery("select r from BranchStaffRole r where r.branchId=:branch and r.userId=:user and r.isActive=true and r.deletedAt is null",BranchStaffRole.class)
      .setParameter("branch",branchId).setParameter("user",userId).getResultList();
    for(BranchStaffRole role:roles){role.isActive=false;role.deletedAt=OffsetDateTime.now();}
    em.flush();
  }
  @Override public boolean staffExists(UUID userId,UUID branchId){return !activeStaffRoles(userId,branchId).isEmpty();}
}
