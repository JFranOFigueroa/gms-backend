package com.gymback.modules.site.services;

import com.gymback.modules.site.dto.SiteDtos.SiteSettingsPatch;
import com.gymback.modules.site.model.SiteMediaAsset;
import com.gymback.modules.site.model.SiteSettings;
import com.gymback.modules.site.repositories.SiteMediaAssetRepository;
import com.gymback.modules.site.repositories.SiteSettingsRepository;
import com.gymback.security.Actor;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.services.AuditService;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.util.*;

public class SiteSettingsService {
  private final SiteSettingsRepository repo;
  private final SiteMediaAssetRepository media;
  private final AuditService audit;
  private final AuthorizationService auth;

  public SiteSettingsService(SiteSettingsRepository repo, SiteMediaAssetRepository media,
      AuditService audit, AuthorizationService auth) {
    this.repo = repo; this.media = media; this.audit = audit; this.auth = auth;
  }

  @Transactional(readOnly = true)
  public Map<String, Object> get() {
    auth.siteAdmin();
    return view(repo.require());
  }

  @Transactional
  public Map<String, Object> patch(SiteSettingsPatch data) {
    Actor actor = auth.siteAdmin();
    SiteSettings s = repo.require();
    if (data == null) throw new DomainException("Datos de configuración obligatorios");
    if (data.siteName() != null) {
      if (data.siteName().isBlank()) throw new DomainException("El nombre del sitio es obligatorio");
      s.setSiteName(data.siteName().strip());
    }
    if (data.shortName() != null) s.setShortName(data.shortName().strip());
    if (data.logoAssetId() != null) {
      SiteMediaAsset asset = media.get(data.logoAssetId(), null)
        .orElseThrow(() -> new DomainException("La imagen del logo no existe"));
      if (!"image".equals(asset.getMediaType())) throw new DomainException("El logo debe ser una imagen");
      s.setLogoAssetId(asset.getId());
    }
    if (data.contactEmail() != null) s.setContactEmail(data.contactEmail().strip());
    if (data.contactPhone() != null) s.setContactPhone(data.contactPhone().strip());
    if (data.address() != null) s.setAddress(data.address().strip());
    if (data.timezone() != null) {
      try { ZoneId.of(data.timezone()); }
      catch (RuntimeException ex) { throw new DomainException("Zona horaria inválida"); }
      s.setTimezone(data.timezone());
    }
    if (data.currency() != null) {
      if (!data.currency().matches("[A-Z]{3}")) throw new DomainException("Moneda inválida");
      s.setCurrency(data.currency());
    }
    if (data.themePreset() != null) {
      if (data.themePreset().isBlank()) throw new DomainException("Tema inválido");
      s.setThemePreset(data.themePreset());
    }
    if (data.themeOverrides() != null) s.setThemeOverrides(data.themeOverrides());
    audit.setAuditFields(s, actor.id(), false);
    return view(repo.save(s));
  }

  private Map<String, Object> view(SiteSettings s) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", s.getId());
    m.put("siteName", s.getSiteName());
    m.put("shortName", s.getShortName());
    m.put("logoAssetId", s.getLogoAssetId());
    m.put("logoUrl", s.getLogoAssetId() == null ? null : "/api/v1/public/media/" + s.getLogoAssetId());
    m.put("contactEmail", s.getContactEmail());
    m.put("contactPhone", s.getContactPhone());
    m.put("address", s.getAddress());
    m.put("timezone", s.getTimezone());
    m.put("currency", s.getCurrency());
    m.put("themePreset", s.getThemePreset());
    m.put("themeOverrides", s.getThemeOverrides());
    return m;
  }
}
