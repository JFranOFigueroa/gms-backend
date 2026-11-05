package com.gymback.modules.users.interfaces;
import java.util.*;
public interface IUserRepository {
  Optional<Map<String,Object>> byEmail(String email);
  Optional<Map<String,Object>> byId(UUID id);
  Optional<Map<String,Object>> byEmailInGym(String email,UUID gymId);
  Map<String,Object> create(String name,String email,String phone,String passwordHash,String status);
  List<Map<String,Object>> list(String query,String status,int limit,int offset);
  long count(String query,String status);
  Map<String,Object> patch(UUID id,String name,String email,String phone);
  Map<String,Object> status(UUID id,String status);
  boolean isPlatformAdmin(UUID id);
  long activeAdmins();
  void lockAdminStatusChange();
}
