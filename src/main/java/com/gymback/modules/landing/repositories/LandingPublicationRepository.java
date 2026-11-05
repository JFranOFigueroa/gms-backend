package com.gymback.modules.landing.repositories;
import com.gymback.modules.landing.interfaces.ILandingPublicationRepository;
import com.gymback.modules.gyms.model.Gym;
import com.gymback.modules.landing.model.LandingBlock;
import com.gymback.modules.media.model.MediaAsset;
import com.gymback.shared.persistence.model.GymConfiguration;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class LandingPublicationRepository implements ILandingPublicationRepository {
  private final EntityManager em;
  public LandingPublicationRepository(EntityManager em){this.em=em;}
  private Gym gym(UUID id,boolean lock){
    return lock?em.find(Gym.class,id,LockModeType.PESSIMISTIC_WRITE):em.find(Gym.class,id);
  }
  @Override public Optional<State> state(UUID gymId,boolean lock){
    Gym g=gym(gymId,lock);
    return g==null?Optional.empty():Optional.of(new State(g.getId(),g.getName(),g.getSlug(),g.getStatus()));
  }
  @Override public Map<String,Object> draft(UUID gymId){
    Gym g=Optional.ofNullable(gym(gymId,false))
      .orElseThrow(()->new com.gymback.shared.exceptions.NotFoundException("Gimnasio no encontrado"));
    GymConfiguration config=Optional.ofNullable(em.find(GymConfiguration.class,gymId))
      .orElseThrow(()->new com.gymback.shared.exceptions.NotFoundException("Configuración no encontrada"));
    Map<String,Object> gView=new LinkedHashMap<>();gView.put("name",g.getName());gView.put("shortName",g.getShortName());
    gView.put("slug",g.getSlug());gView.put("logoUrl",g.getLogoUrl());
    Map<String,Object> theme=new LinkedHashMap<>();theme.put("preset",config.themePreset);theme.put("overrides",config.themeOverrides);
    var dbBlocks=em.createQuery("select b from LandingBlock b where b.gymId=:gym and b.enabled=true order by b.sortOrder,b.id",LandingBlock.class)
      .setParameter("gym",gymId).getResultList();
    List<Map<String,Object>> blocks=new ArrayList<>();
    for(LandingBlock b:dbBlocks){Map<String,Object> m=new LinkedHashMap<>();m.put("id",b.getId());m.put("type",b.getBlockType());
      m.put("order",b.getSortOrder());m.put("enabled",true);m.put("data",b.getBlockData());blocks.add(m);}
    Map<String,Object> page=new LinkedHashMap<>();page.put("gym",gView);page.put("theme",theme);page.put("blocks",blocks);return page;
  }
  @Override public void slug(UUID gymId,String slug){Gym g=require(gymId);g.setSlug(slug);em.flush();}
  @Override public void logo(UUID gymId,UUID assetId){Gym g=require(gymId);g.setLogoUrl("/api/v1/public/media/"+assetId);em.flush();}
  private Gym require(UUID gym){return Optional.ofNullable(em.find(Gym.class,gym))
    .orElseThrow(()->new com.gymback.shared.exceptions.NotFoundException("Gimnasio no encontrado"));}
  private List<Map<String,Object>> publicImages(UUID gymId,List<Map<String,Object>> blocks){
    Set<UUID> references=new HashSet<>();
    for(var b:blocks){
      if(!(b.get("data") instanceof Map<?,?> data))continue;
      for(String key:List.of("imageAssetId","coverAssetId","backgroundAssetId"))addId(references,data.get(key));
      if(data.get("slides") instanceof Collection<?> slides)for(Object slide:slides)addId(references,slide);
    }
    if(references.isEmpty())return List.of();
    List<MediaAsset> assets=em.createQuery("select a from MediaAsset a where a.gymId=:gym and a.id in :ids and a.mediaType='image'",MediaAsset.class)
      .setParameter("gym",gymId).setParameter("ids",references).getResultList();
    List<Map<String,Object>> result=new ArrayList<>();
    for(MediaAsset a:assets){Map<String,Object> m=new LinkedHashMap<>();m.put("id",a.getId());m.put("name",a.getName());
      m.put("url",a.getStorageKey()==null?a.getExternalUrl():"/api/v1/public/media/"+a.getId());result.add(m);}
    return result;
  }
  private void addId(Set<UUID> refs,Object value){
    if(value instanceof String text)try{refs.add(UUID.fromString(text));}catch(IllegalArgumentException ignored){}
  }
  @SuppressWarnings("unchecked") @Override public Optional<Map<String,Object>> publicPage(String slug){
    var matches=em.createQuery("select g from Gym g where lower(g.slug)=lower(:slug) and g.status='active'",Gym.class)
      .setParameter("slug",slug).setMaxResults(1).getResultList();
    if(matches.isEmpty())return Optional.empty();
    UUID gymId=matches.get(0).getId();Map<String,Object> page=draft(gymId);
    page.put("mediaAssets",publicImages(gymId,(List<Map<String,Object>>)page.get("blocks")));
    return Optional.of(page);
  }
}
