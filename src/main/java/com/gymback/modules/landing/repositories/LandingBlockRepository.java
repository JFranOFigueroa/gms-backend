package com.gymback.modules.landing.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.landing.model.LandingBlock;
import com.gymback.modules.landing.interfaces.ILandingBlockRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
@Repository
public class LandingBlockRepository extends BaseCRUD<LandingBlock> implements ILandingBlockRepository {
  public LandingBlockRepository(EntityManager em){super(em,LandingBlock.class,true);}
}
