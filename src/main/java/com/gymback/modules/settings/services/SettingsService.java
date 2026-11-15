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
 @Transactional(readOnly=true)public Map<String,Object> read(UUID branch){auth.require(branch,"owner","trainer");return repo.settings(branch);}
 @Transactional public Map<String,Object> patch(UUID branch,SettingsPatch d){auth.require(branch,"owner");
   Map<String,Object> fields=new LinkedHashMap<>();
   if(d.allowMultipleNormalClassesPerDay()!=null)fields.put("allow_multiple_normal_classes_per_day",d.allowMultipleNormalClassesPerDay());
   if(d.guestBookingLimit()!=null){if(d.guestBookingLimit()<0)throw new DomainException("Límite inválido");fields.put("guest_booking_limit",d.guestBookingLimit());}
   if(d.checkinQrTtlSeconds()!=null){if(d.checkinQrTtlSeconds()<5||d.checkinQrTtlSeconds()>300)throw new DomainException("QR TTL inválido");fields.put("checkin_qr_ttl_seconds",d.checkinQrTtlSeconds());}
   return repo.patch(branch,fields);
 }
 @Transactional(readOnly=true)public List<Map<String,Object>> modules(UUID branch){auth.require(branch,"owner","trainer","member");return repo.modules(branch);}
 @Transactional public List<Map<String,Object>> toggle(UUID branch,String key,ModulePatch d){Actor a=auth.platformAdmin();
   if(!Set.of("calendar","routines","checkin","pos").contains(key)||d.enabled()==null)throw new DomainException("Módulo inválido");
   repo.toggle(branch,key,d.enabled(),a.id());return repo.modules(branch);
 }
}
