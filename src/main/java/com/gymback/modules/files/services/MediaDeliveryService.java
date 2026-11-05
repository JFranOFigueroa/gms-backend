package com.gymback.modules.files.services;
import com.gymback.modules.files.interfaces.IMediaAccessRepository;
import com.gymback.modules.media.interfaces.IMediaAssetRepository;
import com.gymback.modules.media.model.MediaAsset;
import com.gymback.security.*;
import com.gymback.shared.exceptions.NotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class MediaDeliveryService {
  private final IMediaAssetRepository assets;
  private final IMediaAccessRepository access;
  private final LocalImageStorage storage;
  private final AuthorizationService authorization;
  public MediaDeliveryService(IMediaAssetRepository assets,IMediaAccessRepository access,LocalImageStorage storage,AuthorizationService authorization){
    this.assets=assets;this.access=access;this.storage=storage;this.authorization=authorization;
  }
  @Transactional(readOnly=true)
  public ResponseEntity<Resource> privateImage(UUID gym,UUID id){
    Actor actor=authorization.require(gym,"owner","trainer","member");
    if(!actor.platformAdmin() && !authorization.hasRole(gym,"owner") && !authorization.hasRole(gym,"trainer")
        && !access.isOwnMemberPhoto(gym,actor.id(),id))throw new NotFoundException("Imagen no encontrada");
    return serve(assets.get(id,gym).orElseThrow(()->new NotFoundException("Imagen no encontrada")),false);
  }
  @Transactional(readOnly=true)
  public ResponseEntity<Resource> publicImage(UUID id){
    // Buscar por ID global de forma limitada; el gym asociado se valida mediante la referencia pública.
    // El servicio no autoriza usando solo un UUID arbitrario.
    MediaAsset asset=assets.findPublicCandidate(id).orElseThrow(()->new NotFoundException("Imagen no encontrada"));
    if(!access.isPublicImage(asset.getGymId(),id))throw new NotFoundException("Imagen no encontrada");
    return serve(asset,true);
  }
  private ResponseEntity<Resource> serve(MediaAsset asset,boolean isPublic){
    if(!"image".equals(asset.getMediaType()) || asset.getStorageKey()==null)throw new NotFoundException("Imagen no encontrada");
    Resource resource=storage.read(asset.getStorageKey());
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(asset.getMimeType()))
      .header("X-Content-Type-Options","nosniff")
      .cacheControl(isPublic?CacheControl.maxAge(java.time.Duration.ofHours(1)):CacheControl.noStore())
      .body(resource);
  }
}
