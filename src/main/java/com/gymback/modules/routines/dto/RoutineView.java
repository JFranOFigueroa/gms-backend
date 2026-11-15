package com.gymback.modules.routines.dto;
import com.gymback.modules.routines.model.Routine;
import java.util.*;
public record RoutineView(UUID id,UUID branchId,String name,String description,String level,String goal,boolean published,List<RoutineDayRequest> days){
 public static RoutineView of(Routine r,List<RoutineDayRequest> days){return new RoutineView(r.getId(),r.getBranchId(),r.getName(),r.getDescription(),r.getLevel(),r.getGoal(),Boolean.TRUE.equals(r.getPublished()),days);}
}
