package com.gymback.modules.members.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.members.model.GymMember;
import com.gymback.modules.members.interfaces.IMemberRepository;
import com.gymback.shared.persistence.model.*;
import com.gymback.modules.plans.model.MembershipPlan;
import com.gymback.modules.media.model.MediaAsset;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class MemberRepository extends BaseCRUD<GymMember> implements IMemberRepository {
  public MemberRepository(EntityManager em){super(em,GymMember.class,true);}
  private static final String DETAIL="select m,u,s,p,a from GymMember m " +
    "join UserAccount u on u.id=m.userId " +
    "left join MemberSubscription s on s.gymId=m.gymId and s.memberId=m.id and s.status='active' " +
    "left join MembershipPlan p on p.id=s.planId and p.gymId=m.gymId " +
    "left join MediaAsset a on a.id=m.photoAssetId and a.gymId=m.gymId " +
    "where m.gymId=:gym";
  private Map<String,Object> detail(Object[] row){
    GymMember m=(GymMember)row[0];UserAccount u=(UserAccount)row[1];MemberSubscription s=(MemberSubscription)row[2];
    MembershipPlan p=(MembershipPlan)row[3];MediaAsset a=(MediaAsset)row[4];
    String url=a==null?null:(a.getStorageKey()==null?a.getExternalUrl():"/api/v1/gyms/"+m.gymId+"/media-assets/"+a.getId()+"/content");
    Map<String,Object> v=new LinkedHashMap<>();
    v.put("id",m.id);v.put("gym_id",m.gymId);v.put("user_id",m.userId);v.put("code",m.memberCode);
    v.put("name",u.fullName);v.put("email",u.email);v.put("phone",u.phone);v.put("birth_date",m.birthDate);
    v.put("address",m.address);v.put("emergency_contact_name",m.emergencyContactName);
    v.put("emergency_contact_phone",m.emergencyContactPhone);v.put("photo_asset_id",m.photoAssetId);
    v.put("photo_url",url);v.put("notes",m.notes);v.put("start_date",m.joinedOn);v.put("active",m.isActive);
    v.put("created_at",m.createdAt);v.put("next_payment_date",s==null?null:s.nextDueDate);
    v.put("last_payment_date",s==null?null:s.lastPaymentDate);v.put("payment_modality",p==null?null:p.getBillingPeriod());
    return v;
  }
  @Override public List<Map<String,Object>> listDetails(UUID gymId,int offset,int limit){
    if(gymId==null || limit<1 || limit>100 || offset<0)throw new IllegalArgumentException("Paginación inválida");
    return em.createQuery(DETAIL+" order by m.createdAt desc",Object[].class).setParameter("gym",gymId)
      .setFirstResult(offset).setMaxResults(limit).getResultStream().map(this::detail).toList();
  }
  @Override public Map<String,Object> own(UUID gymId,UUID userId){
    var row=em.createQuery(DETAIL+" and m.userId=:user",Object[].class).setParameter("gym",gymId)
      .setParameter("user",userId).setMaxResults(1).getResultStream().findFirst();
    if(row.isEmpty())return null;
    Map<String,Object> v=new LinkedHashMap<>(detail(row.get()));
    var status=em.createQuery("select a.accessStatus from MemberAccessView a where a.gymId=:gym and a.userId=:user",String.class)
      .setParameter("gym",gymId).setParameter("user",userId).getResultStream().findFirst().orElse("unknown");
    v.put("accessStatus",status);v.put("photoUrl",v.get("photo_url"));return v;
  }
  @Override public Map<String,Object> detail(UUID gymId,UUID memberId){
    return em.createQuery(DETAIL+" and m.id=:member",Object[].class).setParameter("gym",gymId)
      .setParameter("member",memberId).setMaxResults(1).getResultStream().findFirst().map(this::detail).orElse(null);
  }
}
