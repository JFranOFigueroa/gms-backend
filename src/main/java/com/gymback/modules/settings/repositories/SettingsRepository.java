package com.gymback.modules.settings.repositories;
import com.gymback.modules.settings.interfaces.ISettingsRepository;
import com.gymback.shared.persistence.model.*;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository
public class SettingsRepository implements ISettingsRepository {
  private final EntityManager em;
  public SettingsRepository(EntityManager em){this.em=em;}
  private GymConfiguration load(UUID gym){
    return Optional.ofNullable(em.find(GymConfiguration.class,gym))
      .orElseThrow(()->new com.gymback.shared.exceptions.NotFoundException("Configuración no encontrada"));
  }
  private Map<String,Object> mapped(GymConfiguration s){
    Map<String,Object> m=new LinkedHashMap<>();m.put("gym_id",s.gymId);m.put("theme_preset",s.themePreset);
    m.put("theme_overrides",s.themeOverrides);m.put("allow_multiple_normal_classes_per_day",s.allowMultipleNormalClassesPerDay);
    m.put("guest_booking_limit",s.guestBookingLimit);m.put("checkin_qr_ttl_seconds",s.checkinQrTtlSeconds);
    m.put("landing_published",s.landingPublished);m.put("created_at",s.createdAt);m.put("created_by",s.createdBy);
    m.put("updated_at",s.updatedAt);m.put("updated_by",s.updatedBy);m.put("deleted_at",s.deletedAt);m.put("deleted_by",s.deletedBy);
    return m;
  }
  @Override public Map<String,Object> settings(UUID gym){return mapped(load(gym));}
  @Override public Map<String,Object> patch(UUID gym,Map<String,Object> fields){
    GymConfiguration s=load(gym);
    // Whitelist explícita: nunca se construyen columnas SQL desde entrada de usuario.
    for(var entry:fields.entrySet())switch(entry.getKey()){
      case "theme_preset" -> s.themePreset=(String)entry.getValue();
      case "theme_overrides" -> { @SuppressWarnings("unchecked")Map<String,Object> value=(Map<String,Object>)entry.getValue();s.themeOverrides=value; }
      case "allow_multiple_normal_classes_per_day" -> s.allowMultipleNormalClassesPerDay=(Boolean)entry.getValue();
      case "guest_booking_limit" -> s.guestBookingLimit=(Integer)entry.getValue();
      case "checkin_qr_ttl_seconds" -> s.checkinQrTtlSeconds=(Integer)entry.getValue();
      default -> throw new IllegalArgumentException("Campo de configuración no permitido");
    }
    em.flush();return mapped(s);
  }
  @Override public List<Map<String,Object>> modules(UUID gym){
    var rows=em.createQuery("select f from GymFeature f where f.gymId=:gym order by f.moduleKey",GymFeature.class)
      .setParameter("gym",gym).getResultList();
    List<Map<String,Object>> result=new ArrayList<>();
    for(GymFeature f:rows){Map<String,Object> m=new LinkedHashMap<>();m.put("module_key",f.moduleKey);
      m.put("enabled",f.enabled);m.put("options",f.options);result.add(m);}
    return result;
  }
  @Override public void toggle(UUID gym,String key,boolean enabled,UUID actor){
    GymFeature.Key id=new GymFeature.Key();id.gymId=gym;id.moduleKey=key;
    GymFeature f=em.find(GymFeature.class,id);
    if(f==null)throw new com.gymback.shared.exceptions.NotFoundException("Módulo no encontrado");
    f.enabled=enabled;f.updatedBy=actor;em.flush();
  }
}
