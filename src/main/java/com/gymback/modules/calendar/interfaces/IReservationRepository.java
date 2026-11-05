package com.gymback.modules.calendar.interfaces;
import java.util.*;
public interface IReservationRepository {
 record ClassAvailability(UUID id,String category,int capacity,String status,java.time.OffsetDateTime startsAt){}
 Optional<ClassAvailability> lockClass(UUID gym,UUID classId);
 long countActive(UUID gym,UUID classId);
 boolean memberHasAccess(UUID gym,UUID user);
 boolean duplicate(UUID gym,UUID classId,UUID user);
 boolean multipleNormalAllowed(UUID gym);
 boolean bookedNormalSameDay(UUID gym,UUID user,java.time.OffsetDateTime start);
 UUID book(UUID gym,UUID classId,UUID user);
 boolean cancelOwn(UUID gym,UUID reservation,UUID user);
   List<Map<String,Object>> listForClass(UUID gym,UUID classId);
  List<Map<String,Object>> own(UUID gym,UUID user,java.time.LocalDate from,java.time.LocalDate to,String status,int offset,int limit);
  long ownCount(UUID gym,UUID user,java.time.LocalDate from,java.time.LocalDate to,String status);
  Optional<Map<String,Object>> availability(UUID gym,UUID classId,UUID user);
}
