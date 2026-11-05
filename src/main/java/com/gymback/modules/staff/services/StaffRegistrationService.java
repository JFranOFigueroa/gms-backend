package com.gymback.modules.staff.services;
import com.gymback.modules.staff.dto.*;
import com.gymback.modules.users.dto.UserCreateRequest;
import com.gymback.modules.users.services.UserService;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.DomainException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class StaffRegistrationService {
 private final AuthorizationService auth;private final UserService users;private final StaffService staff;
 public StaffRegistrationService(AuthorizationService auth,UserService users,StaffService staff){this.auth=auth;this.users=users;this.staff=staff;}
 @Transactional public Map<String,Object> register(UUID gym,StaffRegistrationRequest r){
   auth.require(gym,"owner");users.enabled();
   if(r==null||r.user()==null||!"trainer".equals(r.role()))throw new DomainException("Solo se puede registrar trainer");
   if(r.user().status()!=null&&!"active".equals(r.user().status()))throw new DomainException("Trainer debe estar activo");
   UserCreateRequest data=new UserCreateRequest(r.user().fullName(),r.user().email(),r.user().phone(),r.user().password(),"active");
   var user=users.createForGym(data);
   var link=staff.add(gym,new StaffRequest((UUID)user.get("id"),"trainer"));
   return Map.of("user",user,"staff",link);
 }
}
