package com.gymback.modules.checkin.interfaces;
import java.time.*;
import java.util.*;
public interface ICheckinRepository {
 record Access(String status,String timezone){}
 record Attempt(boolean allowed,String reason,LocalDate visitDate,OffsetDateTime occurredAt,String fingerprint){}
 record EventPoint(OffsetDateTime occurredAt,boolean allowed){}
 Access evaluate(UUID branch,UUID member);
 UUID record(UUID branch,UUID member,LocalDate visitDate,boolean allowed,String reason,UUID actor);
 List<Map<String,Object>> list(UUID branch,int limit);
 List<Map<String,Object>> own(UUID branch,UUID user,LocalDate from,LocalDate to,int limit);
 boolean alreadyAllowed(UUID branch,UUID member,LocalDate date);
 Optional<UUID> memberIdForUser(UUID branch,UUID user);
 boolean lockMember(UUID branch,UUID member);
 void lockDevice(UUID branch,UUID device);
 Optional<Attempt> priorAttempt(UUID branch,String keyHash);
 Attempt recordQr(UUID branch,UUID member,UUID device,String method,LocalDate visitDate,boolean allowed,
               String reason,UUID actor,String keyHash,String fingerprint);
 List<EventPoint> eventsForStats(UUID branch,LocalDate from,LocalDate to);
 long countRecent(UUID branch,UUID member,UUID device,OffsetDateTime since);
 long countRecentCodeAttempts(UUID branch,UUID device,OffsetDateTime since);
}
