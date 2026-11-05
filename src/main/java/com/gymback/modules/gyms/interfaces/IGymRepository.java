package com.gymback.modules.gyms.interfaces;
import com.gymback.shared.interfaces.IBaseCrudRepository;
import com.gymback.modules.gyms.model.Gym;
public interface IGymRepository extends IBaseCrudRepository<Gym> {
  boolean slugExists(String slug,java.util.UUID exceptGymId);
}
