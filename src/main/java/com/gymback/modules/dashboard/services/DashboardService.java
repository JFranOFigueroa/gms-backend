package com.gymback.modules.dashboard.services;
import com.gymback.modules.dashboard.interfaces.IDashboardRepository;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.DomainException;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
public class DashboardService {
 private final IDashboardRepository repo;private final AuthorizationService auth;
 public DashboardService(IDashboardRepository repo,AuthorizationService auth){this.repo=repo;this.auth=auth;}
 @Transactional(readOnly=true)public Map<String,Object> summary(UUID gym,LocalDate from,LocalDate to){
   auth.require(gym,"owner");
   Map<String,Object> details=repo.gym(gym);String tz=(String)details.get("timezone");
   LocalDate now=LocalDate.now(ZoneId.of(tz));
   LocalDate start=from==null?now.withDayOfMonth(1):from,end=to==null?now.withDayOfMonth(now.lengthOfMonth()):to;
   if(start.isAfter(end)||start.isBefore(now.minusYears(5))||end.isAfter(now.plusYears(2)))throw new DomainException("Rango de fechas inválido");
   Map<String,Object> response=new LinkedHashMap<>();response.put("gymId",gym);response.put("from",start);response.put("to",end);
   response.put("timezone",tz);response.put("currency",details.get("currency").toString().trim());
   response.putAll(repo.stats(gym,start,end));return response;
 }
}
