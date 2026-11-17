package com.gymback.modules.site.services;

import com.gymback.modules.branches.interfaces.IBranchRepository;
import com.gymback.modules.branches.model.Branch;
import com.gymback.modules.site.model.*;
import com.gymback.modules.site.repositories.*;
import com.gymback.security.IIdentityRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Contrato anónimo de la única landing del sitio. No acepta slug ni branchId. */
public class PublicSiteService {
  private final SiteSettingsRepository settings;
  private final SiteLandingBlockRepository blocks;
  private final SiteMediaAssetRepository media;
  private final IBranchRepository branches;
  private final IIdentityRepository identity;

  public PublicSiteService(SiteSettingsRepository settings, SiteLandingBlockRepository blocks,
      SiteMediaAssetRepository media, IBranchRepository branches, IIdentityRepository identity) {
    this.settings = settings; this.blocks = blocks; this.media = media; this.branches = branches;
    this.identity = identity;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> page() {
    SiteSettings s = settings.find().orElse(null);
    Map<String, Object> site = new LinkedHashMap<>();
    site.put("name", s == null ? "GymBack" : s.getSiteName());
    site.put("shortName", s == null ? null : s.getShortName());
    site.put("logoUrl", s == null || s.getLogoAssetId() == null
      ? null : "/api/v1/public/media/" + s.getLogoAssetId());
    site.put("contactEmail", s == null ? null : s.getContactEmail());
    site.put("contactPhone", s == null ? null : s.getContactPhone());
    site.put("address", s == null ? null : s.getAddress());
    site.put("timezone", s == null ? "America/Mexico_City" : s.getTimezone());
    site.put("currency", s == null ? "MXN" : s.getCurrency());

    Map<String, Object> theme = new LinkedHashMap<>();
    theme.put("preset", s == null ? "crossfit-dark" : s.getThemePreset());
    theme.put("overrides", s == null ? Map.of() : s.getThemeOverrides());

    List<Map<String, Object>> blockViews = new ArrayList<>();
    for (SiteLandingBlock b : blocks.enabledOrdered()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", b.getId());
      m.put("type", b.getBlockType());
      m.put("order", b.getSortOrder());
      m.put("enabled", true);
      m.put("data", b.getBlockData());
      blockViews.add(m);
    }

    List<Map<String, Object>> assetViews = new ArrayList<>();
    for (SiteMediaAsset a : media.images()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", a.getId());
      m.put("name", a.getName());
      m.put("url", a.getUrl());
      assetViews.add(m);
    }

    // El kiosco necesita una sucursal concreta. La landing no elige una por el
    // visitante: expone las activas para que el selector sea explícito.
    List<Map<String, Object>> branchViews = new ArrayList<>();
    for (Branch b : branches.active()) {
      // El selector solo ofrece sucursales donde el check-in está realmente
      // habilitado, respetando tanto branch_modules como el modo global.
      if (!identity.isModuleEnabled(b.getId(), "checkin")) continue;
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("id", b.getId());
      m.put("name", b.getName());
      m.put("code", b.getCode());
      branchViews.add(m);
    }

    Map<String, Object> page = new LinkedHashMap<>();
    page.put("site", site);
    page.put("theme", theme);
    page.put("blocks", blockViews);
    page.put("mediaAssets", assetViews);
    page.put("branches", branchViews);
    return page;
  }
}
