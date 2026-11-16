package com.gymback.modules.site.repositories;

import com.gymback.modules.site.model.SiteMediaAsset;
import com.gymback.shared.repositories.BaseCRUD;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class SiteMediaAssetRepository extends BaseCRUD<SiteMediaAsset> {
  public SiteMediaAssetRepository(EntityManager em) { super(em, SiteMediaAsset.class, false); }
  public List<SiteMediaAsset> images() {
    return em.createQuery(
        "select a from SiteMediaAsset a where a.deletedAt is null and a.mediaType='image' order by a.createdAt desc",
        SiteMediaAsset.class).getResultList();
  }
}
