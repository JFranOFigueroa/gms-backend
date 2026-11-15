package com.gymback.modules.site.repositories;

import com.gymback.modules.site.model.SiteSettings;
import com.gymback.shared.exceptions.NotFoundException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public class SiteSettingsRepository {
  private final EntityManager em;
  public SiteSettingsRepository(EntityManager em) { this.em = em; }
  public Optional<SiteSettings> find() {
    return Optional.ofNullable(em.find(SiteSettings.class, SiteSettings.SINGLETON_ID));
  }
  public SiteSettings require() {
    return find().orElseThrow(() -> new NotFoundException("Configuración del sitio no encontrada"));
  }
  public SiteSettings save(SiteSettings settings) {
    SiteSettings managed = em.merge(settings);
    em.flush();
    return managed;
  }
}
