package com.gymback.security;
import com.gymback.security.dto.GymAccess;
import java.util.*;

public interface IIdentityRepository {
  Optional<UserCredentials> findCredentialsByEmail(String email);
  Optional<UserCredentials> findCredentialsById(UUID userId);
  boolean isPlatformAdmin(UUID userId);
  /** Inserta el rol global técnico al crear la primera cuenta administradora. */
  void insertPlatformAdmin(UUID userId);
  boolean isGymActive(UUID gymId);
  boolean isModuleEnabled(UUID gymId,String module);
  Set<String> activeRoles(UUID userId,UUID gymId);
  /** Gimnasios visibles con roles efectivos para el selector de la pantalla de login. */
  List<GymAccess> gymsForUser(UUID userId, boolean platformAdmin);
  /** Solo enlaza usuarios EXISTENTES; el alta global se realiza por SQL fuera de la API. */
  void setDefaultGymIfMissing(UUID userId,UUID gymId);
  void assignStaff(UUID userId,UUID gymId,String role,UUID assignedBy);
  void deactivateStaff(UUID userId,UUID gymId);
  Set<String> activeStaffRoles(UUID userId,UUID gymId);
  boolean staffExists(UUID userId,UUID gymId);
  record UserCredentials(UUID id,String name,String email,String passwordHash,String status,UUID defaultGymId) {}
}
