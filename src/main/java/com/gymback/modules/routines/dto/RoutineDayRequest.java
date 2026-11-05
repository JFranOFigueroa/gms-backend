package com.gymback.modules.routines.dto;
import java.util.List;
public record RoutineDayRequest(String day,List<Exercise> exercises){
  public record Exercise(String name,String sets,String reps,String rest,String notes){}
}
