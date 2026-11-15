package com.gymback.modules.site.repositories;

import com.gymback.modules.site.model.SiteLandingBlock;
import com.gymback.shared.repositories.BaseCRUD;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class SiteLandingBlockRepository extends BaseCRUD<SiteLandingBlock> {
  public SiteLandingBlockRepository(EntityManager em) { super(em, SiteLandingBlock.class, false); }
  public List<SiteLandingBlock> listOrdered(int offset, int limit) {
    return em.createQuery(
        "select b from SiteLandingBlock b where b.deletedAt is null order by b.sortOrder, b.id",
        SiteLandingBlock.class)
      .setFirstResult(offset).setMaxResults(limit).getResultList();
  }
  public List<SiteLandingBlock> enabledOrdered() {
    return em.createQuery(
        "select b from SiteLandingBlock b where b.deletedAt is null and b.enabled=true order by b.sortOrder, b.id",
        SiteLandingBlock.class).getResultList();
  }
}
