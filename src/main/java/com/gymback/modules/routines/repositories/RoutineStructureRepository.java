package com.gymback.modules.routines.repositories;
import com.gymback.modules.routines.interfaces.IRoutineStructureRepository;
import com.gymback.modules.routines.dto.RoutineDayRequest;
import com.gymback.shared.persistence.model.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;
@Repository
public class RoutineStructureRepository implements IRoutineStructureRepository {
  private final EntityManager em;
  public RoutineStructureRepository(EntityManager em){this.em=em;}
  @Override public void replace(UUID gym,UUID routine,List<RoutineDayRequest> days){
    if(days.size()>14)throw new IllegalArgumentException("Demasiados días");
    // Validar todo ANTES de archivar la estructura vigente.
    for(var d:days){
      if(d.day()==null || d.day().isBlank())throw new IllegalArgumentException("Día sin nombre");
      if(d.exercises()!=null){
        if(d.exercises().size()>60)throw new IllegalArgumentException("Demasiados ejercicios");
        for(var e:d.exercises())if(e.name()==null || e.name().isBlank())throw new IllegalArgumentException("Ejercicio sin nombre");
      }
    }
    softDelete(gym,routine);
    int pos=0;
    for(var d:days){
      RoutineDay day=new RoutineDay();day.gymId=gym;day.routineId=routine;day.dayOrder=++pos;day.dayLabel=d.day();
      em.persist(day);
      if(d.exercises()!=null){int order=0;for(var e:d.exercises()){
        RoutineExercise ex=new RoutineExercise();ex.gymId=gym;ex.routineDayId=day.id;ex.exerciseOrder=++order;
        ex.exerciseName=e.name();ex.setsLabel=e.sets();ex.repsLabel=e.reps();ex.restLabel=e.rest();ex.notes=e.notes();em.persist(ex);
      }}
    }
    em.flush();
  }
  @Override public void softDelete(UUID gym,UUID routine){
    var days=em.createQuery("select d from RoutineDay d where d.gymId=:gym and d.routineId=:routine",RoutineDay.class)
      .setParameter("gym",gym).setParameter("routine",routine).getResultList();
    if(days.isEmpty())return;
    List<UUID> ids=days.stream().map(d->d.id).toList();
    var exercises=em.createQuery("select e from RoutineExercise e where e.gymId=:gym and e.routineDayId in :ids",RoutineExercise.class)
      .setParameter("gym",gym).setParameter("ids",ids).getResultList();
    OffsetDateTime now=OffsetDateTime.now();
    for(RoutineExercise e:exercises)e.deletedAt=now;
    for(RoutineDay d:days)d.deletedAt=now;
    // El índice único parcial de V4 permite reutilizar el orden solo al finalizar el UPDATE.
    em.flush();
  }
  @Override public List<RoutineDayRequest> list(UUID gym,UUID routine){
    List<RoutineDay> days=em.createQuery("select d from RoutineDay d where d.gymId=:gym and d.routineId=:routine order by d.dayOrder",RoutineDay.class)
      .setParameter("gym",gym).setParameter("routine",routine).getResultList();
    if(days.isEmpty())return List.of();
    var ids=days.stream().map(d->d.id).toList();
    var exercises=em.createQuery("select e from RoutineExercise e where e.gymId=:gym and e.routineDayId in :ids order by e.routineDayId,e.exerciseOrder",RoutineExercise.class)
      .setParameter("gym",gym).setParameter("ids",ids).getResultList();
    Map<UUID,List<RoutineDayRequest.Exercise>> grouped=new HashMap<>();
    for(RoutineExercise e:exercises)grouped.computeIfAbsent(e.routineDayId,ignored->new ArrayList<>())
      .add(new RoutineDayRequest.Exercise(e.exerciseName,e.setsLabel,e.repsLabel,e.restLabel,e.notes));
    return days.stream().map(d->new RoutineDayRequest(d.dayLabel,grouped.getOrDefault(d.id,List.of()))).toList();
  }
}
