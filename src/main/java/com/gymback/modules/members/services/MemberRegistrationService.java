package com.gymback.modules.members.services;
import com.gymback.modules.members.dto.*;
import com.gymback.modules.users.dto.UserCreateRequest;
import com.gymback.modules.users.services.UserService;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.DomainException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class MemberRegistrationService {
 private final AuthorizationService auth;private final UserService users;private final MemberService members;
 public MemberRegistrationService(AuthorizationService auth,UserService users,MemberService members){this.auth=auth;this.users=users;this.members=members;}
 @Transactional public Map<String,Object> register(UUID gym,MemberRegistrationRequest r){
   auth.require(gym,"owner");users.enabled();
   if(r==null || r.user()==null)throw new DomainException("Usuario obligatorio");
   if(r.user().status()!=null && !"active".equals(r.user().status()))throw new DomainException("Miembro nuevo debe estar activo");
   UserCreateRequest u=new UserCreateRequest(r.user().fullName(),r.user().email(),r.user().phone(),r.user().password(),"active");
   var created=users.createForGym(u);UUID id=(UUID)created.get("id");
   MemberRegistrationRequest.MemberData m=r.member();
   var member=members.add(gym,new MemberRequest(id,m==null?null:m.code(),m==null?null:m.birthDate(),
     m==null?null:m.address(),m==null?null:m.emergencyContactName(),m==null?null:m.emergencyContactPhone(),
     m==null?null:m.photoAssetId(),m==null?null:m.notes(),m==null?null:m.startDate(),m==null?true:m.active()));
   return Map.of("user",created,"member",member);
 }
}
