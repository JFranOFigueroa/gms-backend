package com.gymback.modules.staff.services;
import com.gymback.modules.staff.dto.StaffRequest;
import com.gymback.modules.staff.interfaces.IStaffRepository;
import com.gymback.security.*;
import com.gymback.shared.exceptions.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

public class StaffService {
  private final AuthorizationService auth;
  private final IIdentityRepository accounts;
  private final IStaffRepository repo;
  public StaffService(AuthorizationService auth,IIdentityRepository accounts,IStaffRepository repo) {
    this.auth=auth;this.accounts=accounts;this.repo=repo;
  }
  @Transactional(readOnly=true)
  public List<Map<String,Object>> all(UUID branch) {auth.require(branch,"owner");return repo.list(branch);}
  @Transactional(readOnly=true)
  public Map<String,Object> one(UUID branch,UUID user) {
    auth.require(branch,"owner");
    return Optional.ofNullable(repo.get(branch,user)).orElseThrow(()->new NotFoundException("Staff no encontrado"));
  }
  private void verifyRole(UUID branch,String role) {
    if (!Set.of("trainer","owner").contains(role)) throw new DomainException("Rol no permitido");
    Actor actor=auth.require(branch,"owner");
    if(!actor.platformAdmin() && !"trainer".equals(role))
      throw new AccessDeniedException("Un owner solo puede asignar trainer");
  }
  @Transactional
  public Map<String,Object> add(UUID branch,StaffRequest d) {
    if(d==null || d.userId()==null || d.role()==null) throw new DomainException("userId y role son obligatorios");
    verifyRole(branch,d.role());
    var user=accounts.findCredentialsById(d.userId())
      .orElseThrow(()->new DomainException("Usuario global no encontrado"));
    if(!"active".equals(user.status())) throw new DomainException("Solo se vinculan usuarios globales activos");
    if(accounts.activeStaffRoles(d.userId(),branch).contains(d.role()))
      throw new DomainException("El usuario ya tiene este rol activo en la sucursal");
    accounts.assignStaff(d.userId(),branch,d.role(),auth.actor().id());
    return one(branch,d.userId());
  }
  @Transactional
  public Map<String,Object> change(UUID branch,UUID id,StaffRequest d) {
    auth.require(branch,"owner");
    if(d==null || d.role()==null) throw new DomainException("role obligatorio");
    if(d.userId()!=null) throw new DomainException("No es posible cambiar userId desde staff");
    Set<String> roles=accounts.activeStaffRoles(id,branch);
    if(roles.isEmpty()) throw new NotFoundException("Staff no encontrado");
    // /staff/{userId} identifica a una persona, no a un rol: no adivinar qué rol reemplazar.
    if(roles.size()!=1) throw new DomainException("Usuario con múltiples roles: edición manual pendiente de definir");
    String anterior=roles.iterator().next();
    verifyRole(branch,anterior);verifyRole(branch,d.role());
    if(!anterior.equals(d.role())) {
      accounts.deactivateStaff(id,branch);
      accounts.assignStaff(id,branch,d.role(),auth.actor().id());
    }
    return one(branch,id);
  }
  @Transactional
  public void remove(UUID branch,UUID id) {
    auth.require(branch,"owner");
    Set<String> roles=accounts.activeStaffRoles(id,branch);
    if(roles.isEmpty()) throw new NotFoundException("Staff no encontrado");
    // Un owner normal no puede desactivar a otro owner, incluso con múltiples roles.
    for(String role:roles) verifyRole(branch,role);
    accounts.deactivateStaff(id,branch);
  }
}
