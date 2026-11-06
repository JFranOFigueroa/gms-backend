package com.gymback.security;
import java.time.OffsetDateTime;
import java.util.UUID;
/** Principal de kiosco, nunca debe confundirse con JWT del usuario. */
public record KioskPrincipal(UUID gymId,UUID deviceId,UUID sessionId,String deviceName,OffsetDateTime expiresAt,String sessionSecret){}
