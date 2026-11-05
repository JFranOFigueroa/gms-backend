package com.gymback.modules.media.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.media.model.MediaAsset;
import com.gymback.modules.media.interfaces.IMediaAssetRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
@Repository
public class MediaAssetRepository extends BaseCRUD<MediaAsset> implements IMediaAssetRepository {
  public MediaAssetRepository(EntityManager em){super(em,MediaAsset.class,true);}
  @Override public java.util.Optional<MediaAsset> findPublicCandidate(java.util.UUID id){
    return em.createQuery("SELECT a FROM MediaAsset a WHERE a.id=:id AND a.mediaType='image'",MediaAsset.class)
      .setParameter("id",id).getResultStream().findFirst();
  }
}
