package com.gymback.modules.routines.interfaces;
import com.gymback.modules.routines.dto.RoutineDayRequest;
import java.util.*;
public interface IRoutineStructureRepository {
 void replace(UUID gym,UUID routine,List<RoutineDayRequest> days);
 List<RoutineDayRequest> list(UUID gym,UUID routine);
  void softDelete(java.util.UUID gym,java.util.UUID routine);
}
