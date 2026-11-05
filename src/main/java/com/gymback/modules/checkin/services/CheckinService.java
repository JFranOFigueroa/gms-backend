package com.gymback.modules.checkin.services;
import com.gymback.modules.checkin.dto.CheckinRequest;
import com.gymback.modules.checkin.interfaces.ICheckinRepository;
import com.gymback.security.*;
import com.gymback.shared.exceptions.DomainException;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
public class CheckinService {
 private final ICheckinRepository repo;private final AuthorizationService auth;
 public CheckinService(ICheckinRepository repo,AuthorizationService auth){this.repo=repo;this.auth=auth;}
 @Transactional(readOnly=true)public List<Map<String,Object>> audit(UUID gym,int limit){auth.requireModule(gym,"checkin","owner","trainer");return repo.list(gym,limit);}
 @Transactional(readOnly=true)public Map<String,Object> own(UUID gym,LocalDate from,LocalDate to,int limit){
   Actor a=auth.requireModule(gym,"checkin","member");
   if(limit<1 || limit>100 || from!=null && to!=null && from.isAfter(to))throw new DomainException("Rango inválido");
   return Map.of("items",repo.own(gym,a.id(),from,to,limit));
 }
 @Transactional public Map<String,Object> manual(UUID gym,CheckinRequest req){
   auth.requireModule(gym,"checkin","owner","trainer");if(req.memberId()==null)throw new DomainException("memberId obligatorio");
   var access=repo.evaluate(gym,req.memberId());LocalDate today=LocalDate.now(ZoneId.of(access.timezone()));
   boolean allowed=access.status().equals("allowed");String reason=access.status();
   if(allowed && repo.alreadyAllowed(gym,req.memberId(),today)){allowed=false;reason="duplicate";}
   // Nunca insertar memberId foráneo para 'not_found'. El evento negado sigue auditable.
   UUID event=repo.record(gym,reason.equals("not_found")?null:req.memberId(),today,allowed,reason,auth.actor().id());
   return Map.of("eventId",event,"allowed",allowed,"reason",reason,"visitDate",today);
 }
}
