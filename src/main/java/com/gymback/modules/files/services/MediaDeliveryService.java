package com.gymback.modules.files.services;

import com.gymback.modules.site.model.SiteMediaAsset;
import com.gymback.modules.site.repositories.SiteMediaAssetRepository;
import com.gymback.shared.exceptions.NotFoundException;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.UUID;

/** Entrega pública de imágenes del sitio. Las fotos de miembros usan su propio endpoint privado. */
@Service
public class MediaDeliveryService {
  private final SiteMediaAssetRepository assets;
  private final LocalImageStorage storage;

  public MediaDeliveryService(SiteMediaAssetRepository assets, LocalImageStorage storage) {
    this.assets = assets; this.storage = storage;
  }

  @Transactional(readOnly = true)
  public ResponseEntity<Resource> publicImage(UUID id) {
    SiteMediaAsset asset = assets.get(id, null).orElseThrow(() -> new NotFoundException("Imagen no encontrada"));
    if (!"image".equals(asset.getMediaType())) throw new NotFoundException("Imagen no encontrada");
    if (asset.getStorageKey() == null) {
      if (asset.getExternalUrl() == null) throw new NotFoundException("Imagen no encontrada");
      return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(asset.getExternalUrl())).build();
    }
    Resource resource = storage.read(asset.getStorageKey());
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(asset.getMimeType()))
      .header("X-Content-Type-Options", "nosniff")
      .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)))
      .body(resource);
  }
}
