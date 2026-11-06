package com.gymback.modules.checkin.interfaces;
import java.time.*;
import java.util.*;
public interface ICheckinRepository {
 record Access(String status,String timezone){}
 record Attempt(boolean allowed,String reason,LocalDate visitDate,OffsetDateTime occurredAt,String fingerprint){}
 record EventPoint(OffsetDateTime occurredAt,boolean allowed){}
 Access evaluate(UUID gym,UUID member);
 UUID record(UUID gym,UUID member,LocalDate visitDate,boolean allowed,String reason,UUID actor);
 List<Map<String,Object>> list(UUID gym,int limit);
 List<Map<String,Object>> own(UUID gym,UUID user,LocalDate from,LocalDate to,int limit);
 boolean alreadyAllowed(UUID gym,UUID member,LocalDate date);
 Optional<UUID> memberIdForUser(UUID gym,UUID user);
 boolean lockMember(UUID gym,UUID member);
 void lockDevice(UUID gym,UUID device);
 Optional<Attempt> priorAttempt(UUID gym,String keyHash);
 Attempt recordQr(UUID gym,UUID member,UUID device,String method,LocalDate visitDate,boolean allowed,
               String reason,UUID actor,String keyHash,String fingerprint);
 List<EventPoint> eventsForStats(UUID gym,LocalDate from,LocalDate to);
 long countRecent(UUID gym,UUID member,UUID device,OffsetDateTime since);
 long countRecentCodeAttempts(UUID gym,UUID device,OffsetDateTime since);
}
