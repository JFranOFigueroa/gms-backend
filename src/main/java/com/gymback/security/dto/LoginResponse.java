package com.gymback.security.dto;
import java.util.List;
/** Conserva token/tokenType/user/gyms y añade refreshToken y expiresIn. */
public record LoginResponse(String token, String tokenType, AuthUser user, List<GymAccess> gyms,
                            String refreshToken, long expiresIn) {}
