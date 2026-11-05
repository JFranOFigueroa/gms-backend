package com.gymback.security;
import com.gymback.shared.exceptions.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class AuthorizationService {
  private final IIdentityRepository users;
  private final com.gymback.shared.services.AuditContext auditContext;
  public AuthorizationService(IIdentityRepository users,com.gymback.shared.services.AuditContext auditContext){this.users=users;this.auditContext=auditContext;}
  public Actor actor() {
    if(!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken jwt)) throw new AccessDeniedException("No autenticado");
    UUID id=UUID.fromString(jwt.getToken().getSubject());
    var user=users.findCredentialsById(id).orElseThrow(()->new AccessDeniedException("Usuario inexistente"));
    if(!user.status().equals("active"))throw new AccessDeniedException("Usuario inactivo");
    auditContext.setActor(id);
    return new Actor(id,users.isPlatformAdmin(id));
  }
  public Actor platformAdmin() {Actor a=actor();if(!a.platformAdmin())throw new AccessDeniedException("Solo admin global");return a;}
  public Actor require(UUID gymId,String...roles) {
    if(gymId==null)throw new IllegalArgumentException("gymId obligatorio");
    Actor a=actor();if(!users.isGymActive(gymId))throw new AccessDeniedException("Gimnasio inactivo");if(a.platformAdmin())return a;
    Set<String> actual=users.activeRoles(a.id(),gymId);
    if(Arrays.stream(roles).noneMatch(actual::contains))throw new AccessDeniedException("Sin permiso en gimnasio");
    return a;
  }
  public boolean hasRole(UUID gymId,String role){Actor a=actor();return a.platformAdmin() || users.activeRoles(a.id(),gymId).contains(role);}
  public Actor requireModule(UUID gym,String module,String...roles){
    Actor a=require(gym,roles);
    if(!users.isModuleEnabled(gym,module))throw new AccessDeniedException("Módulo deshabilitado");
    return a;
  }
}
