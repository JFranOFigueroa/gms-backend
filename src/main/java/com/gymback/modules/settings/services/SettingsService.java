package com.gymback.modules.settings.services;
import com.gymback.modules.settings.dto.*;
import com.gymback.modules.settings.interfaces.ISettingsRepository;
import com.gymback.security.*;
import com.gymback.shared.exceptions.DomainException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class SettingsService {
 private final ISettingsRepository repo;private final AuthorizationService auth;
 public SettingsService(ISettingsRepository repo,AuthorizationService auth){this.repo=repo;this.auth=auth;}
 @Transactional(readOnly=true)public Map<String,Object> read(UUID gym){auth.require(gym,"owner","trainer");return repo.settings(gym);}
 @Transactional public Map<String,Object> patch(UUID gym,SettingsPatch d){auth.require(gym,"owner");
   Map<String,Object> fields=new LinkedHashMap<>();
   if(d.themePreset()!=null)fields.put("theme_preset",d.themePreset());
   if(d.themeOverrides()!=null)fields.put("theme_overrides",d.themeOverrides());
   if(d.allowMultipleNormalClassesPerDay()!=null)fields.put("allow_multiple_normal_classes_per_day",d.allowMultipleNormalClassesPerDay());
   if(d.guestBookingLimit()!=null){if(d.guestBookingLimit()<0)throw new DomainException("Límite inválido");fields.put("guest_booking_limit",d.guestBookingLimit());}
   if(d.checkinQrTtlSeconds()!=null){if(d.checkinQrTtlSeconds()<5||d.checkinQrTtlSeconds()>300)throw new DomainException("QR TTL inválido");fields.put("checkin_qr_ttl_seconds",d.checkinQrTtlSeconds());}
   if(d.landingPublished()!=null)throw new DomainException("La página del gimnasio está disponible automáticamente; no se modifica su visibilidad aquí");
   return repo.patch(gym,fields);
 }
 @Transactional(readOnly=true)public List<Map<String,Object>> modules(UUID gym){auth.require(gym,"owner","trainer","member");return repo.modules(gym);}
 @Transactional public List<Map<String,Object>> toggle(UUID gym,String key,ModulePatch d){Actor a=auth.platformAdmin();
   if(!Set.of("calendar","routines","checkin","pos").contains(key)||d.enabled()==null)throw new DomainException("Módulo inválido");
   repo.toggle(gym,key,d.enabled(),a.id());return repo.modules(gym);
 }
}
