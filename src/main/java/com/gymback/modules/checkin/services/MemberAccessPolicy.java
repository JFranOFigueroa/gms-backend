package com.gymback.modules.checkin.services;
import com.gymback.modules.checkin.interfaces.ICheckinRepository;
import java.time.*;
import java.util.*;
/** Política única para manual, escaneo de QR de gimnasio y QR de miembro. */
public class MemberAccessPolicy {
 private final ICheckinRepository repo;
 public MemberAccessPolicy(ICheckinRepository repo){this.repo=repo;}
 public record Decision(LocalDate visitDate,boolean allowed,String reason){}
 public Decision evaluate(UUID gym,UUID member){
  if(member==null||!repo.lockMember(gym,member))return new Decision(LocalDate.now(ZoneId.of("America/Mexico_City")),false,"not_found");
  ICheckinRepository.Access access=repo.evaluate(gym,member);
  String timezone=access.timezone()==null?"America/Mexico_City":access.timezone();
  LocalDate date=LocalDate.now(ZoneId.of(timezone));
  String reason=access.status();boolean allowed="allowed".equals(reason);
  if(allowed&&repo.alreadyAllowed(gym,member,date)){allowed=false;reason="duplicate";}
  return new Decision(date,allowed,allowed?"granted":reason);
 }
}
