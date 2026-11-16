package com.gymback.security;
import com.gymback.security.dto.BranchAccess;
import java.util.*;

public interface IIdentityRepository {
  Optional<UserCredentials> findCredentialsByEmail(String email);
  Optional<UserCredentials> findCredentialsById(UUID userId);
  boolean isPlatformAdmin(UUID userId);
  /** Inserta el rol global técnico al crear la primera cuenta administradora. */
  void insertPlatformAdmin(UUID userId);
  boolean isBranchActive(UUID branchId);
  boolean isModuleEnabled(UUID branchId,String module);
  Set<String> activeRoles(UUID userId,UUID branchId);
  /** Sucursales visibles con roles efectivos para el selector de la pantalla de login. */
  List<BranchAccess> branchesForUser(UUID userId, boolean platformAdmin);
  /** Solo enlaza usuarios EXISTENTES; el alta global se realiza por SQL fuera de la API. */
  void setDefaultBranchIfMissing(UUID userId,UUID branchId);
  void assignStaff(UUID userId,UUID branchId,String role,UUID assignedBy);
  void deactivateStaff(UUID userId,UUID branchId);
  Set<String> activeStaffRoles(UUID userId,UUID branchId);
  boolean staffExists(UUID userId,UUID branchId);
  record UserCredentials(UUID id,String name,String email,String passwordHash,String status,UUID defaultBranchId) {}
}
