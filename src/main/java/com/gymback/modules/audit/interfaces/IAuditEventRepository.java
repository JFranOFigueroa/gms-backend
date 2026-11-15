package com.gymback.modules.audit.interfaces;
import java.util.*;
public interface IAuditEventRepository {
  List<Map<String,Object>> list(UUID branchId,int offset,int limit);
  List<Map<String,Object>> all(UUID branchId,int offset,int limit);
}
