package com.gymback.modules.calendar.services;
import com.gymback.modules.calendar.dto.ReservationRequest;
import com.gymback.modules.calendar.model.ClassCategories;
import com.gymback.modules.calendar.interfaces.IReservationRepository;
import com.gymback.security.*;
import com.gymback.shared.exceptions.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.*;
public class ReservationService {
 private final IReservationRepository repo;private final AuthorizationService auth;
 public ReservationService(IReservationRepository repo,AuthorizationService auth){this.repo=repo;this.auth=auth;}
 @Transactional(readOnly=true)public List<Map<String,Object>> list(UUID gym,UUID classId){auth.requireModule(gym,"calendar","owner","trainer");return repo.listForClass(gym,classId);}
 @Transactional(readOnly=true) public Map<String,Object> own(UUID gym,LocalDate from,LocalDate to,String status,int offset,int limit){
   Actor actor=auth.requireModule(gym,"calendar","owner","trainer","member");
   if(offset<0 || limit<1 || limit>100 || (from!=null && to!=null && from.isAfter(to)))throw new DomainException("Rango/paginación inválidos");
   if(status!=null && !Set.of("booked","cancelled","attended","no_show").contains(status))throw new DomainException("Estado inválido");
   var items=repo.own(gym,actor.id(),from,to,status,offset,limit);
   return Map.of("items",items,"total",repo.ownCount(gym,actor.id(),from,to,status),"offset",offset,"limit",limit);
 }
 @Transactional(readOnly=true) public Map<String,Object> availability(UUID gym,UUID classId){
   Actor actor=auth.requireModule(gym,"calendar","owner","trainer","member");
   Map<String,Object> answer=repo.availability(gym,classId,actor.id()).orElseThrow(()->new NotFoundException("Clase no encontrada"));
   if(auth.hasRole(gym,"member") && !auth.hasRole(gym,"owner")&&!auth.hasRole(gym,"trainer")&&!repo.memberHasAccess(gym,actor.id()))
       answer.put("bookable",false);
   return answer;
 }
 @Transactional public Map<String,Object> book(UUID gym,ReservationRequest req){
   Actor actor=auth.requireModule(gym,"calendar","owner","trainer","member");
   if(req.classId()==null)throw new DomainException("classId obligatorio");
   var c=repo.lockClass(gym,req.classId()).orElseThrow(()->new NotFoundException("Clase no encontrada"));
   if(!c.status().equals("scheduled") || !c.startsAt().isAfter(OffsetDateTime.now()))throw new DomainException("Clase no disponible");
   if(repo.duplicate(gym,c.id(),actor.id()))throw new DomainException("Reserva duplicada");
   if(repo.countActive(gym,c.id())>=c.capacity())throw new DomainException("Cupo agotado");
   if(auth.hasRole(gym,"member") && !auth.hasRole(gym,"owner") && !auth.hasRole(gym,"trainer") && !repo.memberHasAccess(gym,actor.id()))throw new DomainException("Membresía sin acceso");
   if(ClassCategories.countsAsRegular(c.category()) && !repo.multipleNormalAllowed(gym) && repo.bookedNormalSameDay(gym,actor.id(),c.startsAt()))throw new DomainException("Ya tienes clase normal reservada ese día");
   UUID reservation=repo.book(gym,c.id(),actor.id());return Map.of("id",reservation,"classId",c.id(),"status","booked");
 }
 @Transactional public void cancel(UUID gym,UUID id){Actor actor=auth.requireModule(gym,"calendar","owner","trainer","member");if(!repo.cancelOwn(gym,id,actor.id()))throw new NotFoundException("Reserva propia activa no encontrada");}
}
