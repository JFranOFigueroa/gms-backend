package com.gymback.modules.calendar.interfaces;
import java.util.*;
public interface IReservationRepository {
 record ClassAvailability(UUID id,String category,int capacity,String status,java.time.OffsetDateTime startsAt){}
 Optional<ClassAvailability> lockClass(UUID branch,UUID classId);
 long countActive(UUID branch,UUID classId);
 boolean memberHasAccess(UUID branch,UUID user);
 boolean duplicate(UUID branch,UUID classId,UUID user);
 boolean multipleNormalAllowed(UUID branch);
 boolean bookedNormalSameDay(UUID branch,UUID user,java.time.OffsetDateTime start);
 UUID book(UUID branch,UUID classId,UUID user);
 boolean cancelOwn(UUID branch,UUID reservation,UUID user);
   List<Map<String,Object>> listForClass(UUID branch,UUID classId);
  List<Map<String,Object>> own(UUID branch,UUID user,java.time.LocalDate from,java.time.LocalDate to,String status,int offset,int limit);
  long ownCount(UUID branch,UUID user,java.time.LocalDate from,java.time.LocalDate to,String status);
  Optional<Map<String,Object>> availability(UUID branch,UUID classId,UUID user);
}
