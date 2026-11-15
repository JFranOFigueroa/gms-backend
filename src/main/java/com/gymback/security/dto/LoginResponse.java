package com.gymback.security.dto;
import java.util.List;
/** Conserva token/tokenType/user/branches y añade refreshToken y expiresIn. */
public record LoginResponse(String token, String tokenType, AuthUser user, List<BranchAccess> branches,
                            String refreshToken, long expiresIn) {}
