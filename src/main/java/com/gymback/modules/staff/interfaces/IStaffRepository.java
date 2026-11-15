package com.gymback.modules.staff.interfaces;
import java.util.*;
public interface IStaffRepository {
  List<Map<String,Object>> list(UUID branchId);
  Map<String,Object> get(UUID branchId,UUID userId);
}
