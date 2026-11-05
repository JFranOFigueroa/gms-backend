package com.gymback.modules.calendar.interfaces;
import com.gymback.shared.interfaces.IBaseCrudRepository;
import com.gymback.modules.calendar.model.GymClass;
public interface IGymClassRepository extends IBaseCrudRepository<GymClass> {
 java.util.List<GymClass> calendar(java.util.UUID gymId,java.time.LocalDate from,java.time.LocalDate to,String status,int offset,int limit);
}
