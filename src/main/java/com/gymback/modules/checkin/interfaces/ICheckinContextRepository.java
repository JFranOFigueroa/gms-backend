package com.gymback.modules.checkin.interfaces;
import java.util.*;
public interface ICheckinContextRepository {
 record GymContext(UUID id,String name,String slug,String timezone,int qrTtl){}
 Optional<GymContext> bySlug(String slug);
 Optional<GymContext> byId(UUID id);
}
