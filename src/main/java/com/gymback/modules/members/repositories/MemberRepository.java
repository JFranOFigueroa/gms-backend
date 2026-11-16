package com.gymback.modules.members.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.members.model.BranchMember;
import com.gymback.modules.members.model.MemberPhoto;
import com.gymback.modules.members.interfaces.IMemberRepository;
import com.gymback.shared.persistence.model.*;
import com.gymback.modules.plans.model.MembershipPlan;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class MemberRepository extends BaseCRUD<BranchMember> implements IMemberRepository {
  public MemberRepository(EntityManager em){super(em,BranchMember.class,true);}
  private static final String DETAIL="select m,u,s,p,ph from BranchMember m " +
    "join UserAccount u on u.id=m.userId " +
    "left join MemberSubscription s on s.branchId=m.branchId and s.memberId=m.id and s.status='active' " +
    "left join MembershipPlan p on p.id=s.planId and p.branchId=m.branchId " +
    "left join MemberPhoto ph on ph.memberId=m.id and ph.branchId=m.branchId and ph.deletedAt is null " +
    "where m.branchId=:branch";
  private Map<String,Object> detail(Object[] row){
    BranchMember m=(BranchMember)row[0];UserAccount u=(UserAccount)row[1];MemberSubscription s=(MemberSubscription)row[2];
    MembershipPlan p=(MembershipPlan)row[3];MemberPhoto ph=(MemberPhoto)row[4];
    String url=ph==null?null:"/api/v1/branches/"+m.branchId+"/members/"+m.id+"/photo";
    Map<String,Object> v=new LinkedHashMap<>();
    v.put("id",m.id);v.put("branch_id",m.branchId);v.put("user_id",m.userId);v.put("code",m.memberCode);
    v.put("name",u.fullName);v.put("email",u.email);v.put("phone",u.phone);v.put("birth_date",m.birthDate);
    v.put("address",m.address);v.put("emergency_contact_name",m.emergencyContactName);
    v.put("emergency_contact_phone",m.emergencyContactPhone);
    v.put("photo_url",url);v.put("notes",m.notes);v.put("start_date",m.joinedOn);v.put("active",m.isActive);
    v.put("created_at",m.createdAt);v.put("next_payment_date",s==null?null:s.nextDueDate);
    v.put("last_payment_date",s==null?null:s.lastPaymentDate);v.put("payment_modality",p==null?null:p.getBillingPeriod());
    return v;
  }
  @Override public List<Map<String,Object>> listDetails(UUID branchId,int offset,int limit){
    if(branchId==null || limit<1 || limit>100 || offset<0)throw new IllegalArgumentException("Paginación inválida");
    return em.createQuery(DETAIL+" order by m.createdAt desc",Object[].class).setParameter("branch",branchId)
      .setFirstResult(offset).setMaxResults(limit).getResultStream().map(this::detail).toList();
  }
  @Override public Map<String,Object> own(UUID branchId,UUID userId){
    var row=em.createQuery(DETAIL+" and m.userId=:user",Object[].class).setParameter("branch",branchId)
      .setParameter("user",userId).setMaxResults(1).getResultStream().findFirst();
    if(row.isEmpty())return null;
    Map<String,Object> v=new LinkedHashMap<>(detail(row.get()));
    var status=em.createQuery("select a.accessStatus from MemberAccessView a where a.branchId=:branch and a.userId=:user",String.class)
      .setParameter("branch",branchId).setParameter("user",userId).getResultStream().findFirst().orElse("unknown");
    v.put("accessStatus",status);v.put("photoUrl",v.get("photo_url"));return v;
  }
  @Override public Map<String,Object> detail(UUID branchId,UUID memberId){
    return em.createQuery(DETAIL+" and m.id=:member",Object[].class).setParameter("branch",branchId)
      .setParameter("member",memberId).setMaxResults(1).getResultStream().findFirst().map(this::detail).orElse(null);
  }
}
