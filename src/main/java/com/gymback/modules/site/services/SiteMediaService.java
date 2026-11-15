package com.gymback.modules.site.services;

import com.gymback.modules.files.services.LocalImageStorage;
import com.gymback.modules.site.dto.SiteDtos.SiteMediaAssetRequest;
import com.gymback.modules.site.model.SiteMediaAsset;
import com.gymback.modules.site.repositories.SiteMediaAssetRepository;
import com.gymback.security.Actor;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.services.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

public class SiteMediaService extends BaseService<SiteMediaAsset> {
  private final SiteMediaAssetRepository assets;
  private final AuthorizationService auth;
  private final LocalImageStorage files;

  public SiteMediaService(SiteMediaAssetRepository repo, AuditService audit,
      AuthorizationService auth, LocalImageStorage files) {
    super(repo, audit); this.assets = repo; this.auth = auth; this.files = files;
  }

  @Transactional(readOnly = true)
  public List<SiteMediaAsset> all() {
    auth.siteAdmin();
    return assets.images();
  }

  @Transactional(readOnly = true)
  public SiteMediaAsset one(UUID id) {
    auth.siteAdmin();
    return getOr404(id, null);
  }

  @Transactional
  public SiteMediaAsset add(SiteMediaAssetRequest req) {
    Actor actor = auth.siteAdmin();
    if (req == null || req.name() == null || req.name().isBlank())
      throw new DomainException("name es obligatorio");
    SiteMediaAsset e = new SiteMediaAsset();
    e.setName(req.name().strip());
    e.setMediaType(req.mediaType() == null || req.mediaType().isBlank() ? "image" : req.mediaType());
    e.setExternalUrl(req.externalUrl());
    e.setMimeType(req.mimeType());
    e.setByteSize(req.byteSize());
    e.setUploadedBy(actor.id());
    validate(e);
    return create(e, actor.id());
  }

  @Transactional
  public SiteMediaAsset upload(MultipartFile file, String name) {
    Actor actor = auth.siteAdmin();
    if (file == null || file.isEmpty()) throw new DomainException("Imagen obligatoria");
    LocalImageStorage.Saved saved = files.save("site", file);
    if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
      org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
        new org.springframework.transaction.support.TransactionSynchronization() {
          @Override public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) files.discard(saved.key());
          }
        });
    }
    SiteMediaAsset e = new SiteMediaAsset();
    e.setName(name == null || name.isBlank() ? "Imagen del sitio" : name.strip());
    e.setMediaType("image");
    e.setStorageKey(saved.key());
    e.setMimeType(saved.mime());
    e.setByteSize(saved.size());
    e.setUploadedBy(actor.id());
    return create(e, actor.id());
  }

  @Transactional
  public SiteMediaAsset change(UUID id, SiteMediaAssetRequest req) {
    Actor actor = auth.siteAdmin();
    SiteMediaAsset e = getOr404(id, null);
    if (req.name() != null) {
      if (req.name().isBlank()) throw new DomainException("name es obligatorio");
      e.setName(req.name().strip());
    }
    if (req.externalUrl() != null) {
      if (e.getStorageKey() != null) throw new DomainException("No se puede reemplazar una imagen local por una URL");
      e.setExternalUrl(req.externalUrl());
    }
    if (req.mimeType() != null) e.setMimeType(req.mimeType());
    if (req.byteSize() != null) e.setByteSize(req.byteSize());
    validate(e);
    return update(e, actor.id());
  }

  @Transactional
  public void remove(UUID id) {
    Actor actor = auth.siteAdmin();
    softDelete(getOr404(id, null), actor.id());
  }

  private void validate(SiteMediaAsset e) {
    if (!Set.of("image", "video", "document").contains(e.getMediaType()))
      throw new DomainException("Tipo de medio inválido");
    if (e.getStorageKey() != null) {
      if (!"image".equals(e.getMediaType()) || e.getExternalUrl() != null)
        throw new DomainException("Archivo local inválido");
    } else if (e.getExternalUrl() == null || !e.getExternalUrl().startsWith("https://")) {
      throw new DomainException("Solo se acepta URL HTTPS");
    }
  }
}
