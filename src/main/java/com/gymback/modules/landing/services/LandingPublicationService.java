package com.gymback.modules.landing.services;
import com.gymback.modules.landing.interfaces.ILandingPublicationRepository;
import com.gymback.modules.gyms.interfaces.IGymRepository;
import com.gymback.modules.gyms.services.SlugPolicy;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
/** Página en vivo del gimnasio. No existe operación de publicar ni despublicar. */
public class LandingPublicationService {
  private final ILandingPublicationRepository repo;
  private final IGymRepository gyms;
  private final com.gymback.modules.media.interfaces.IMediaAssetRepository media;
  private final AuthorizationService auth;
  private final String baseUrl;
  public LandingPublicationService(ILandingPublicationRepository repo,IGymRepository gyms,AuthorizationService auth,
      com.gymback.modules.media.interfaces.IMediaAssetRepository media,
      @Value("${app.public-site-base-url}")String baseUrl){
    this.repo=repo;this.gyms=gyms;this.auth=auth;this.media=media;
    if(!baseUrl.matches("https?://[^/\\s]+(:[0-9]+)?/?"))throw new IllegalStateException("PUBLIC_SITE_BASE_URL requiere una URL http(s) sin ruta");
    this.baseUrl=baseUrl.replaceAll("/$","");
  }
  private ILandingPublicationRepository.State state(UUID gym,boolean lock){
    return repo.state(gym,lock).orElseThrow(()->new NotFoundException("Gimnasio no encontrado"));
  }
  private String share(String slug){return baseUrl+"/g/"+slug;}
  @Transactional(readOnly=true) public Map<String,Object> link(UUID gym){
    auth.require(gym,"owner");var st=state(gym,false);
    Map<String,Object> result=new LinkedHashMap<>();
    result.put("gymId",gym);result.put("gymName",st.gymName());result.put("slug",st.slug());
    result.put("publicUrl","active".equals(st.status())?share(st.slug()):null);
    return result;
  }
  @Transactional public Map<String,Object> changeSlug(UUID gym,String proposed){
    auth.require(gym,"owner");var st=state(gym,true);
    String slug=SlugPolicy.normalize(proposed);
    if(gyms.slugExists(slug,gym))throw new ConflictException("slug_taken","Este enlace ya está en uso");
    repo.slug(gym,slug);
    Map<String,Object> answer=new LinkedHashMap<>();answer.put("gymId",gym);answer.put("slug",slug);
    answer.put("publicUrl","active".equals(st.status())?share(slug):null);return answer;
  }
  /** Solo el propietario decide qué imagen local ya subida será el logo público. */
  @Transactional public Map<String,Object> changeLogo(UUID gym,UUID assetId){
    auth.require(gym,"owner");
    if(assetId==null)throw new DomainException("Selecciona una imagen");
    var asset=media.get(assetId,gym).orElseThrow(()->new NotFoundException("Imagen no encontrada"));
    if(!"image".equals(asset.getMediaType()) || asset.getStorageKey()==null)
      throw new DomainException("El logo debe ser una imagen local del gimnasio");
    repo.logo(gym,assetId);
    return Map.of("logoUrl","/api/v1/public/media/"+assetId);
  }
  @Transactional(readOnly=true) public Map<String,Object> publicLanding(String slug){
    if(slug==null || !slug.matches("[a-zA-Z0-9-]{3,80}"))throw new NotFoundException("Página no disponible");
    return repo.publicPage(slug).orElseThrow(()->new NotFoundException("Página no disponible"));
  }
}
