package com.gymback.modules.dashboard.repositories;
import com.gymback.modules.dashboard.interfaces.IDashboardRepository;
import com.gymback.modules.branches.model.Branch;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Repository
public class DashboardRepository implements IDashboardRepository {
  private final EntityManager em;
  public DashboardRepository(EntityManager em){this.em=em;}
  private Branch require(UUID id){return Optional.ofNullable(em.find(Branch.class,id))
    .orElseThrow(()->new com.gymback.shared.exceptions.NotFoundException("Sucursal no encontrado"));}
  @Override public Map<String,Object> branch(UUID id){Branch g=require(id);Map<String,Object> map=new LinkedHashMap<>();
    map.put("timezone",g.getTimezone());map.put("currency",g.getCurrency());return map;}
  private record Range(OffsetDateTime start,OffsetDateTime end){}
  private Range range(Branch branch,LocalDate from,LocalDate to){
    ZoneId zone=ZoneId.of(branch.getTimezone());return new Range(
      from==null?null:from.atStartOfDay(zone).toOffsetDateTime(),
      to==null?null:to.plusDays(1).atStartOfDay(zone).toOffsetDateTime());
  }
  private <T> jakarta.persistence.TypedQuery<T> dates(jakarta.persistence.TypedQuery<T> q,Range range){
    if(range.start()!=null)q.setParameter("start",range.start());
    if(range.end()!=null)q.setParameter("end",range.end());return q;
  }
  private String window(String alias,String field,Range range){
    return (range.start()==null?"":" and "+alias+"."+field+">=:start")+
      (range.end()==null?"":" and "+alias+"."+field+"<:end");
  }
  private Map<String,Object> pair(String first,Object a,String second,Object b){
    Map<String,Object> result=new LinkedHashMap<>();result.put(first,a);result.put(second,b);return result;
  }
  @Override public Map<String,Object> stats(UUID branch,LocalDate from,LocalDate to){
    Branch g=require(branch);Range r=range(g,from,to);
    Map<String,Object> result=new LinkedHashMap<>();
    Long members=em.createQuery("select count(m) from BranchMember m where m.branchId=:branch and m.isActive=true",Long.class)
      .setParameter("branch",branch).getSingleResult();result.put("members",Map.of("active",members));
    String chk="select count(e),sum(case when e.allowed=true then 1L else 0L end) from CheckinEntry e where e.branchId=:branch"+
      (from==null?"":" and e.visitDate>=:from")+(to==null?"":" and e.visitDate<=:to");
    var cq=em.createQuery(chk,Object[].class).setParameter("branch",branch);
    if(from!=null)cq.setParameter("from",from);if(to!=null)cq.setParameter("to",to);
    Object[] c=cq.getSingleResult();long total=((Number)c[0]).longValue(),allowed=c[1]==null?0:((Number)c[1]).longValue();
    result.put("checkins",pair("granted",allowed,"denied",total-allowed));
    var sq=dates(em.createQuery("select count(s),sum(s.total) from PosSale s where s.branchId=:branch and s.status='completed'"+
      window("s","soldAt",r),Object[].class).setParameter("branch",branch),r);
    Object[] sales=sq.getSingleResult();result.put("sales",pair("count",sales[0],"revenue",sales[1]==null?BigDecimal.ZERO:sales[1]));
    var pq=dates(em.createQuery("select count(p),sum(p.amount) from PaymentEntry p where p.branchId=:branch and p.status='paid'"+
      window("p","paidAt",r),Object[].class).setParameter("branch",branch),r);
    Object[] payments=pq.getSingleResult();result.put("payments",pair("count",payments[0],"collected",payments[1]==null?BigDecimal.ZERO:payments[1]));
    var gq=dates(em.createQuery("select count(c),sum(case when c.status='scheduled' then 1L else 0L end),"+
      "sum(case when c.status='completed' then 1L else 0L end) from BranchClass c where c.branchId=:branch"+
      window("c","startsAt",r),Object[].class).setParameter("branch",branch),r);
    Object[] classes=gq.getSingleResult();result.put("classes",pair("scheduled",classes[1]==null?0:classes[1],
      "completed",classes[2]==null?0:classes[2]));
    return result;
  }
}
