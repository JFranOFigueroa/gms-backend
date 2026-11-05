package com.gymback.modules.checkin.interfaces;
import java.time.LocalDate;
import java.util.*;
public interface ICheckinRepository {
 record Access(String status,String timezone) {}
 Access evaluate(UUID gym,UUID member);
 UUID record(UUID gym,UUID member,LocalDate visitDate,boolean allowed,String reason,UUID actor);
 List<Map<String,Object>> list(UUID gym,int limit);
 List<Map<String,Object>> own(UUID gym,UUID user,LocalDate from,LocalDate to,int limit);
 boolean alreadyAllowed(UUID gym,UUID member,LocalDate date);
}
