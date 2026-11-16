package com.gymback.modules.settings.services;
import com.gymback.modules.settings.dto.*;
import com.gymback.modules.settings.interfaces.ISettingsRepository;
import com.gymback.modules.site.model.SiteSettings;
import com.gymback.modules.site.repositories.SiteSettingsRepository;
import com.gymback.security.*;
import com.gymback.shared.exceptions.DomainException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class SettingsService {
 private final ISettingsRepository repo;private final SiteSettingsRepository siteSettings;private final AuthorizationService auth;
 public SettingsService(ISettingsRepository repo,SiteSettingsRepository siteSettings,AuthorizationService auth){this.repo=repo;this.siteSettings=siteSettings;this.auth=auth;}
 @Transactional(readOnly=true)public Map<String,Object> read(UUID branch){auth.require(branch,"owner","trainer");return repo.settings(branch);}
 @Transactional public Map<String,Object> patch(UUID branch,SettingsPatch d){auth.require(branch,"owner");
   Map<String,Object> fields=new LinkedHashMap<>();
   if(d.allowMultipleNormalClassesPerDay()!=null)fields.put("allow_multiple_normal_classes_per_day",d.allowMultipleNormalClassesPerDay());
   if(d.guestBookingLimit()!=null){if(d.guestBookingLimit()<0)throw new DomainException("Límite inválido");fields.put("guest_booking_limit",d.guestBookingLimit());}
   if(d.checkinQrTtlSeconds()!=null){if(d.checkinQrTtlSeconds()<5||d.checkinQrTtlSeconds()>300)throw new DomainException("QR TTL inválido");fields.put("checkin_qr_ttl_seconds",d.checkinQrTtlSeconds());}
   return repo.patch(branch,fields);
 }
 @Transactional(readOnly=true)public List<Map<String,Object>> modules(UUID branch){
   auth.require(branch,"owner","trainer","member");
   SiteSettings site=siteSettings.find().orElse(null);
   if(site!=null && site.getDeletedAt()==null && site.isModulesGlobal())return globalModules(site);
   return repo.modules(branch);
 }
 @Transactional public List<Map<String,Object>> toggle(UUID branch,String key,ModulePatch d){Actor a=auth.platformAdmin();
   if(!SiteSettings.MODULE_KEYS.contains(key)||d.enabled()==null)throw new DomainException("Módulo inválido");
   SiteSettings site=siteSettings.find().orElse(null);
   if(site!=null && site.getDeletedAt()==null && site.isModulesGlobal())
     throw new DomainException("Los módulos están configurados globalmente; edítalos desde la configuración del sitio");
   repo.toggle(branch,key,d.enabled(),a.id());return repo.modules(branch);
 }
 private List<Map<String,Object>> globalModules(SiteSettings site){
   List<Map<String,Object>> result=new ArrayList<>();
   Map<String,Object> values=site.getGlobalModules()==null?SiteSettings.defaultGlobalModules():site.getGlobalModules();
   for(String key:SiteSettings.MODULE_KEYS){
     Map<String,Object> m=new LinkedHashMap<>();
     m.put("module_key",key);
     m.put("enabled",Boolean.TRUE.equals(values.get(key)));
     m.put("options",Map.of());
     result.add(m);
   }
   return result;
 }
}
