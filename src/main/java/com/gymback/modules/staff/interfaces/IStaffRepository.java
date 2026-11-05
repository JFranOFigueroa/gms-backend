package com.gymback.modules.staff.interfaces;
import java.util.*;
public interface IStaffRepository {
  List<Map<String,Object>> list(UUID gymId);
  Map<String,Object> get(UUID gymId,UUID userId);
}
