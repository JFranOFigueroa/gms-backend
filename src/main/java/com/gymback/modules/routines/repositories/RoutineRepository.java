package com.gymback.modules.routines.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.routines.model.Routine;
import com.gymback.modules.routines.interfaces.IRoutineRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
@Repository
public class RoutineRepository extends BaseCRUD<Routine> implements IRoutineRepository {
  public RoutineRepository(EntityManager em){super(em,Routine.class,true);}
}
