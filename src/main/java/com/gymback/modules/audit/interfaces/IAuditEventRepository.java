package com.gymback.modules.audit.interfaces;
import java.util.*;
public interface IAuditEventRepository {
  List<Map<String,Object>> list(UUID gymId,int offset,int limit);
  List<Map<String,Object>> all(UUID gymId,int offset,int limit);
}
