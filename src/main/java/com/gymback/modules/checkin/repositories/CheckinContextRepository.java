package com.gymback.modules.checkin.repositories;
import com.gymback.modules.checkin.interfaces.ICheckinContextRepository;
import com.gymback.modules.branches.model.Branch;
import com.gymback.shared.persistence.model.BranchSettings;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class CheckinContextRepository implements ICheckinContextRepository {
 private final EntityManager em;
 public CheckinContextRepository(EntityManager em){this.em=em;}
 public Optional<BranchContext> byCode(String code){
  if(code==null||code.isBlank()||code.length()>150)return Optional.empty();
  return em.createQuery("select g from Branch g where lower(g.code)=lower(:code) and g.status='active'",Branch.class)
   .setParameter("code",code.trim()).setMaxResults(1).getResultStream().findFirst().map(this::context);
 }
 public Optional<BranchContext> byId(UUID branch){return Optional.ofNullable(em.find(Branch.class,branch)).filter(g->"active".equals(g.getStatus())).map(this::context);}
 private BranchContext context(Branch branch){
  BranchSettings cfg=em.find(BranchSettings.class,branch.getId());
  int ttl=cfg==null||cfg.checkinQrTtlSeconds==null?10:cfg.checkinQrTtlSeconds;
  return new BranchContext(branch.getId(),branch.getName(),branch.getCode(),branch.getTimezone(),Math.max(5,Math.min(300,ttl)));
 }
}
