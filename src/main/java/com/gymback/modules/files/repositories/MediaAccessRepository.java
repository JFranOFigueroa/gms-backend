package com.gymback.modules.files.repositories;
import com.gymback.modules.files.interfaces.IMediaAccessRepository;
import com.gymback.modules.gyms.model.Gym;
import com.gymback.modules.landing.model.LandingBlock;
import com.gymback.modules.members.model.GymMember;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class MediaAccessRepository implements IMediaAccessRepository {
  private final EntityManager em;
  public MediaAccessRepository(EntityManager em){this.em=em;}
  @Override public boolean isPublicImage(UUID gymId,UUID assetId){
    Gym gym=em.find(Gym.class,gymId);
    if(gym==null || !"active".equals(gym.getStatus()))return false;
    String id=assetId.toString();
    if(("/api/v1/public/media/"+id).equals(gym.getLogoUrl()))return true;
    List<LandingBlock> blocks=em.createQuery("select b from LandingBlock b where b.gymId=:gym and b.enabled=true",LandingBlock.class)
      .setParameter("gym",gymId).getResultList();
    for(LandingBlock block:blocks){
      Map<String,Object> data=block.getBlockData();if(data==null)continue;
      for(String key:List.of("imageAssetId","coverAssetId","backgroundAssetId"))if(id.equals(data.get(key)))return true;
      Object slides=data.get("slides");if(slides instanceof Collection<?> items && items.contains(id))return true;
    }
    return false;
  }
  @Override public boolean isOwnMemberPhoto(UUID gym,UUID user,UUID asset){
    return em.createQuery("select count(m) from GymMember m where m.gymId=:gym and m.userId=:user and m.photoAssetId=:asset",Long.class)
      .setParameter("gym",gym).setParameter("user",user).setParameter("asset",asset).getSingleResult()>0;
  }
}
