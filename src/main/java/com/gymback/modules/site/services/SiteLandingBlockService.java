package com.gymback.modules.site.services;

import com.gymback.modules.site.dto.SiteDtos.SiteLandingBlockRequest;
import com.gymback.modules.site.model.SiteLandingBlock;
import com.gymback.modules.site.repositories.SiteLandingBlockRepository;
import com.gymback.security.Actor;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.services.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

public class SiteLandingBlockService extends BaseService<SiteLandingBlock> {
  private static final Set<String> TYPES = Set.of("hero", "text", "membership", "carousel");
  private final SiteLandingBlockRepository blocks;
  private final AuthorizationService auth;

  public SiteLandingBlockService(SiteLandingBlockRepository repo, AuditService audit, AuthorizationService auth) {
    super(repo, audit); this.blocks = repo; this.auth = auth;
  }

  @Transactional(readOnly = true)
  public List<SiteLandingBlock> all(int offset, int limit) {
    auth.siteAdmin();
    if (offset < 0 || limit < 1 || limit > 100) throw new IllegalArgumentException("Paginación inválida");
    return blocks.listOrdered(offset, limit);
  }

  @Transactional(readOnly = true)
  public SiteLandingBlock one(UUID id) {
    auth.siteAdmin();
    return getOr404(id, null);
  }

  @Transactional
  public SiteLandingBlock add(SiteLandingBlockRequest req) {
    Actor actor = auth.siteAdmin();
    if (req == null || req.blockType() == null) throw new DomainException("blockType es obligatorio");
    SiteLandingBlock e = new SiteLandingBlock();
    e.setBlockType(req.blockType());
    e.setSortOrder(req.sortOrder() == null ? blocks.listOrdered(0, 100).size() : req.sortOrder());
    e.setEnabled(req.enabled() == null || req.enabled());
    e.setBlockData(req.blockData() == null ? new LinkedHashMap<>() : req.blockData());
    validate(e);
    return create(e, actor.id());
  }

  @Transactional
  public SiteLandingBlock change(UUID id, SiteLandingBlockRequest req) {
    Actor actor = auth.siteAdmin();
    SiteLandingBlock e = getOr404(id, null);
    if (req.blockType() != null) e.setBlockType(req.blockType());
    if (req.sortOrder() != null) e.setSortOrder(req.sortOrder());
    if (req.enabled() != null) e.setEnabled(req.enabled());
    if (req.blockData() != null) e.setBlockData(req.blockData());
    validate(e);
    return update(e, actor.id());
  }

  @Transactional
  public void remove(UUID id) {
    Actor actor = auth.siteAdmin();
    softDelete(getOr404(id, null), actor.id());
  }

  private void validate(SiteLandingBlock e) {
    if (!TYPES.contains(e.getBlockType())) throw new DomainException("Tipo de bloque inválido");
    if (e.getSortOrder() == null || e.getSortOrder() < 0) throw new DomainException("Orden inválido");
    if (e.getBlockData() == null) e.setBlockData(new LinkedHashMap<>());
  }
}
