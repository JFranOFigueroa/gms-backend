package com.gymback.security.dto;
import java.util.UUID;
/** Identidad global: no existe un rol único para un usuario multi-gimnasio. */
public record AuthUser(UUID id, String name, String email, boolean platformAdmin, UUID defaultGymId) {}
