package com.gymback.modules.calendar.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.calendar.model.BranchClass;
import com.gymback.modules.calendar.interfaces.IBranchClassRepository;
import com.gymback.modules.branches.model.Branch;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Repository;
@Repository
public class BranchClassRepository extends BaseCRUD<BranchClass> implements IBranchClassRepository {
  public BranchClassRepository(EntityManager em){super(em,BranchClass.class,true);}
  @Override public List<BranchClass> calendar(UUID branchId,LocalDate from,LocalDate to,String status,int offset,int limit){
    if(branchId==null || offset<0 || limit<1 || limit>100)throw new IllegalArgumentException("Paginación inválida");
    Branch branch=em.find(Branch.class,branchId);
    if(branch==null)return List.of();
    ZoneId zone=ZoneId.of(branch.getTimezone());
    OffsetDateTime start=from==null?null:from.atStartOfDay(zone).toOffsetDateTime();
    OffsetDateTime end=to==null?null:to.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
    String jpql="select c from BranchClass c where c.branchId=:branch"+
      (start==null?"":" and c.startsAt>=:start")+(end==null?"":" and c.startsAt<:end")+
      (status==null || "all".equals(status)?"":" and c.status=:status")+" order by c.startsAt,c.id";
    var query=em.createQuery(jpql,BranchClass.class).setParameter("branch",branchId).setFirstResult(offset).setMaxResults(limit);
    if(start!=null)query.setParameter("start",start);
    if(end!=null)query.setParameter("end",end);
    if(status!=null && !"all".equals(status))query.setParameter("status",status);
    return query.getResultList();
  }
}
