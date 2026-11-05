package com.gymback.modules.users.services;
import com.gymback.modules.users.dto.*;
import com.gymback.modules.users.interfaces.IUserRepository;
import com.gymback.security.AuthorizationService;
import com.gymback.shared.exceptions.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class UserService {
  private final IUserRepository repo;private final AuthorizationService auth;private final PasswordEncoder encoder;
  private final boolean enabled;
  public UserService(IUserRepository repo,AuthorizationService auth,PasswordEncoder encoder,
        @Value("${app.user-api-enabled:false}")boolean enabled){this.repo=repo;this.auth=auth;this.encoder=encoder;this.enabled=enabled;}
  public void enabled(){if(!enabled)throw new AccessDeniedException("Alta de cuentas globales deshabilitada: USER_API_ENABLED");}
  private void admin(){enabled();auth.platformAdmin();}
  private String email(String value){
    if(value==null)throw new DomainException("email obligatorio");
    String e=value.trim().toLowerCase(Locale.ROOT);
    if(e.length()>254 || !e.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))throw new DomainException("email inválido");return e;
  }
  @Transactional public Map<String,Object> createForGym(UserCreateRequest r){
    enabled();if(r==null || r.fullName()==null || r.fullName().isBlank() || r.fullName().length()>250)throw new DomainException("Nombre obligatorio");
    String e=email(r.email());
    if(r.password()==null || r.password().length()<10 || r.password().length()>200)throw new DomainException("Contraseña requiere entre 10 y 200 caracteres");
    String status=r.status()==null?"active":r.status();
    if(!Set.of("active","disabled").contains(status))throw new DomainException("Estado inválido");
    if(repo.byEmail(e).isPresent())throw new ConflictException("email_already_exists","Correo ya registrado; vincula la cuenta existente");
    return repo.create(r.fullName().trim(),e,r.phone(),encoder.encode(r.password()),status);
  }
  @Transactional public Map<String,Object> create(UserCreateRequest r){admin();return createForGym(r);}
  @Transactional(readOnly=true) public Map<String,Object> list(String query,String status,int offset,int limit){admin();
    if(offset<0 || limit<1 || limit>100)throw new DomainException("Paginación 1..100");
    if(status!=null&&!Set.of("active","disabled","invited").contains(status))throw new DomainException("status inválido");
    return Map.of("items",repo.list(query,status,limit,offset),"total",repo.count(query,status),"offset",offset,"limit",limit);
  }
  @Transactional(readOnly=true) public Map<String,Object> one(UUID id){admin();return repo.byId(id).orElseThrow(()->new NotFoundException("Usuario no encontrado"));}
  @Transactional(readOnly=true) public Map<String,Object> search(String email,UUID gymId){
    enabled();var actor=auth.actor();
    String target=email(email);
    if(actor.platformAdmin())return repo.byEmail(target).orElseThrow(()->new NotFoundException("Cuenta no encontrada"));
    if(gymId==null)throw new AccessDeniedException("gymId obligatorio para owner");
    auth.require(gymId,"owner");
    Map<String,Object> found=repo.byEmailInGym(target,gymId).orElseThrow(()->new NotFoundException("Cuenta no encontrada"));
    return Map.of("id",found.get("id"),"fullName",found.get("fullName"),"email",found.get("email"),"status",found.get("status"));
  }
  @Transactional public Map<String,Object> patch(UUID id,UserPatchRequest req){admin();
    if(req==null)throw new DomainException("Body obligatorio");
    repo.byId(id).orElseThrow(()->new NotFoundException("Usuario no encontrado"));
    String email=req.email()==null?null:email(req.email());
    if(email!=null && repo.byEmail(email).map(u->!id.equals(u.get("id"))).orElse(false))throw new ConflictException("email_already_exists","Correo ocupado");
    if(req.fullName()!=null && req.fullName().isBlank())throw new DomainException("Nombre vacío");
    return repo.patch(id,req.fullName(),email,req.phone());
  }
  @Transactional public Map<String,Object> status(UUID id,String status){admin();
    if(status==null || !Set.of("active","disabled").contains(status))throw new DomainException("Estado inválido");
    repo.byId(id).orElseThrow(()->new NotFoundException("Usuario no encontrado"));
    if("disabled".equals(status) && repo.isPlatformAdmin(id)){
      repo.lockAdminStatusChange();
      if(repo.activeAdmins()<=1)
        throw new ConflictException("last_admin","No se puede deshabilitar el último administrador global");
    }
    return repo.status(id,status);
  }
}
