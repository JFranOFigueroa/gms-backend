package com.gymback.modules.media.services;
import com.gymback.modules.media.model.MediaAsset;
import com.gymback.modules.media.interfaces.IMediaAssetRepository;
import com.gymback.modules.media.dto.MediaAssetRequest;
import com.gymback.shared.services.*;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.security.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class MediaAssetService extends BaseService<MediaAsset> {
  private final AuthorizationService auth;
  private final com.gymback.modules.files.services.LocalImageStorage files;
  public MediaAssetService(IMediaAssetRepository repo,AuditService audit,AuthorizationService auth,
     com.gymback.modules.files.services.LocalImageStorage files) {super(repo,audit);this.auth=auth;this.files=files;}
  @Transactional(readOnly=true) public List<MediaAsset> all(UUID gymId,int offset,int limit){ auth.require(gymId,"owner","trainer"); return list(gymId,offset,limit); }
  @Transactional(readOnly=true) public MediaAsset one(UUID gymId,UUID id){ auth.require(gymId,"owner","trainer"); return getOr404(id,gymId); }
  @Transactional public MediaAsset add(UUID gymId,MediaAssetRequest req){
    Actor actor=auth.require(gymId,"owner");
    if(req.name()==null || req.name().isBlank()) throw new DomainException("name es obligatorio");if(req.mediaType()==null || req.mediaType().isBlank()) throw new DomainException("mediaType es obligatorio");if(req.externalUrl()==null || req.externalUrl().isBlank()) throw new DomainException("externalUrl es obligatorio");
    MediaAsset e=new MediaAsset();
    e.setGymId(gymId);
    if(req.name()!=null) e.setName(req.name()); if(req.mediaType()!=null) e.setMediaType(req.mediaType()); if(req.externalUrl()!=null) e.setExternalUrl(req.externalUrl()); if(req.mimeType()!=null) e.setMimeType(req.mimeType()); if(req.byteSize()!=null) e.setByteSize(req.byteSize());

    validate(e);
    return create(e,actor.id());
  }
  @Transactional public MediaAsset upload(UUID gymId,org.springframework.web.multipart.MultipartFile file,String name){
    Actor actor=auth.require(gymId,"owner");
    if(file==null || file.isEmpty())throw new DomainException("Imagen obligatoria");
    var saved=files.save(gymId,file);
    // Si la transacción se revierte, no dejamos un archivo sin registro.
    if(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
      org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
        new org.springframework.transaction.support.TransactionSynchronization(){
          @Override public void afterCompletion(int status){
            if(status!=org.springframework.transaction.support.TransactionSynchronization.STATUS_COMMITTED)files.discard(saved.key());
          }
        });
    }
    MediaAsset asset=new MediaAsset();asset.setGymId(gymId);asset.setName(name==null || name.isBlank()?"Imagen":name.strip());
    asset.setMediaType("image");asset.setStorageKey(saved.key());asset.setMimeType(saved.mime());
    asset.setByteSize(saved.size());asset.setUploadedBy(actor.id());
    return create(asset,actor.id());
  }
  @Transactional public MediaAsset change(UUID gymId,UUID id,MediaAssetRequest req){
    Actor actor=auth.require(gymId,"owner");
    MediaAsset e=getOr404(id,gymId);
    if(req.name()!=null) e.setName(req.name()); if(req.mediaType()!=null) e.setMediaType(req.mediaType()); if(req.externalUrl()!=null) e.setExternalUrl(req.externalUrl()); if(req.mimeType()!=null) e.setMimeType(req.mimeType()); if(req.byteSize()!=null) e.setByteSize(req.byteSize());
    if(e.getStorageKey()!=null && req.externalUrl()!=null)throw new DomainException("No puede reemplazarse una imagen local por una URL; suba una nueva imagen");
    validate(e);
    return update(e,actor.id());
  }
  @Transactional public void remove(UUID gymId,UUID id){
    Actor actor=auth.require(gymId,"owner");
    MediaAsset e=getOr404(id,gymId);
    softDelete(e,actor.id());
  }
  private void validate(MediaAsset e){ if(!Set.of("image","video","document").contains(e.getMediaType()))throw new DomainException("Tipo de medio inválido");
  if(e.getStorageKey()!=null){if(!"image".equals(e.getMediaType()) || e.getExternalUrl()!=null)throw new DomainException("Archivo local inválido");}
  else if(e.getExternalUrl()==null || !e.getExternalUrl().startsWith("https://"))throw new DomainException("Solo se acepta URL HTTPS"); }
}
