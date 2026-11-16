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
  /** Administra la marca/sitio: admin global o propietario de al menos una sucursal activa. */
  public Actor siteAdmin() {
    Actor a=actor();
    if(a.platformAdmin())return a;
    boolean owner=users.branchesForUser(a.id(),false).stream()
      .anyMatch(branch->branch.roles().contains("owner"));
    if(!owner)throw new AccessDeniedException("Solo administradores del sitio");
    return a;
  }
  public Actor require(UUID branchId,String...roles) {
    if(branchId==null)throw new IllegalArgumentException("branchId obligatorio");
    Actor a=actor();if(!users.isBranchActive(branchId))throw new AccessDeniedException("Sucursal inactiva");if(a.platformAdmin())return a;
    Set<String> actual=users.activeRoles(a.id(),branchId);
    if(Arrays.stream(roles).noneMatch(actual::contains))throw new AccessDeniedException("Sin permiso en sucursal");
    return a;
  }
  public boolean hasRole(UUID branchId,String role){Actor a=actor();return a.platformAdmin() || users.activeRoles(a.id(),branchId).contains(role);}
  public Actor requireModule(UUID branch,String module,String...roles){
    Actor a=require(branch,roles);
    if(!users.isModuleEnabled(branch,module))throw new AccessDeniedException("Módulo deshabilitado");
    return a;
  }
}
