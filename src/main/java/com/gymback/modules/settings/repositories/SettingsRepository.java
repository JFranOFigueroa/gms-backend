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
  private BranchSettings load(UUID branch){
    return Optional.ofNullable(em.find(BranchSettings.class,branch))
      .orElseThrow(()->new com.gymback.shared.exceptions.NotFoundException("Configuración no encontrada"));
  }
  private Map<String,Object> mapped(BranchSettings s){
    Map<String,Object> m=new LinkedHashMap<>();m.put("branch_id",s.branchId);
    m.put("allow_multiple_normal_classes_per_day",s.allowMultipleNormalClassesPerDay);
    m.put("guest_booking_limit",s.guestBookingLimit);m.put("checkin_qr_ttl_seconds",s.checkinQrTtlSeconds);
    m.put("created_at",s.createdAt);m.put("created_by",s.createdBy);
    m.put("updated_at",s.updatedAt);m.put("updated_by",s.updatedBy);m.put("deleted_at",s.deletedAt);m.put("deleted_by",s.deletedBy);
    return m;
  }
  @Override public Map<String,Object> settings(UUID branch){return mapped(load(branch));}
  @Override public Map<String,Object> patch(UUID branch,Map<String,Object> fields){
    BranchSettings s=load(branch);
    // Whitelist explícita: nunca se construyen columnas SQL desde entrada de usuario.
    for(var entry:fields.entrySet())switch(entry.getKey()){      case "allow_multiple_normal_classes_per_day" -> s.allowMultipleNormalClassesPerDay=(Boolean)entry.getValue();
      case "guest_booking_limit" -> s.guestBookingLimit=(Integer)entry.getValue();
      case "checkin_qr_ttl_seconds" -> s.checkinQrTtlSeconds=(Integer)entry.getValue();
      default -> throw new IllegalArgumentException("Campo de configuración no permitido");
    }
    em.flush();return mapped(s);
  }
  @Override public List<Map<String,Object>> modules(UUID branch){
    var rows=em.createQuery("select f from BranchModule f where f.branchId=:branch order by f.moduleKey",BranchModule.class)
      .setParameter("branch",branch).getResultList();
    List<Map<String,Object>> result=new ArrayList<>();
    for(BranchModule f:rows){Map<String,Object> m=new LinkedHashMap<>();m.put("module_key",f.moduleKey);
      m.put("enabled",f.enabled);m.put("options",f.options);result.add(m);}
    return result;
  }
  @Override public void toggle(UUID branch,String key,boolean enabled,UUID actor){
    BranchModule.Key id=new BranchModule.Key();id.branchId=branch;id.moduleKey=key;
    BranchModule f=em.find(BranchModule.class,id);
    if(f==null)throw new com.gymback.shared.exceptions.NotFoundException("Módulo no encontrado");
    f.enabled=enabled;f.updatedBy=actor;em.flush();
  }
}
