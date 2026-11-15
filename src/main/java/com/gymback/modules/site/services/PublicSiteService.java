package com.gymback.modules.site.services;

import com.gymback.modules.site.model.*;
import com.gymback.modules.site.repositories.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Contrato anónimo de la única landing del sitio. No acepta slug ni branchId. */
public class PublicSiteService {
  private final SiteSettingsRepository settings;
  private final SiteLandingBlockRepository blocks;
  private final SiteMediaAssetRepository media;

  public PublicSiteService(SiteSettingsRepository settings, SiteLandingBlockRepository blocks,
      SiteMediaAssetRepository media) {
    this.settings = settings; this.blocks = blocks; this.media = media;
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

    Map<String, Object> page = new LinkedHashMap<>();
    page.put("site", site);
    page.put("theme", theme);
    page.put("blocks", blockViews);
    page.put("mediaAssets", assetViews);
    return page;
  }
}
