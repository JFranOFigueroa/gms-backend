package com.gymback.modules.routines.interfaces;
import com.gymback.modules.routines.dto.RoutineDayRequest;
import java.util.*;
public interface IRoutineStructureRepository {
 void replace(UUID branch,UUID routine,List<RoutineDayRequest> days);
 List<RoutineDayRequest> list(UUID branch,UUID routine);
  void softDelete(java.util.UUID branch,java.util.UUID routine);
}
