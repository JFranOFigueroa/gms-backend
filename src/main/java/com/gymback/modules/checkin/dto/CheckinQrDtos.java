package com.gymback.modules.checkin.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.*;
import java.util.*;
/** DTOs nuevos; los DTOs de check-in manual permanecen intactos. */
public final class CheckinQrDtos {
 private CheckinQrDtos(){}
 public record CreateDevice(@NotBlank String deviceName){}
 public record UpdateDevice(@NotNull Boolean active){}
 public record Activate(@NotBlank String slug,@NotBlank String activationCode){}
 public record ScanGym(@NotBlank String kioskToken){}
 public record ScanMember(@NotBlank String memberQr){}
 public record ValidateCode(@NotBlank String accessCode){}
 public record IssuedCode(String accessCode,OffsetDateTime expiresAt,OffsetDateTime serverTime){}
 public record DeviceView(UUID deviceId,String deviceName,boolean active,boolean activated,OffsetDateTime lastSeenAt){}
 public record DeviceCreated(UUID deviceId,String deviceName,boolean active,String activationCode,OffsetDateTime activationExpiresAt){}
 public record IssuedQr(String qrValue,OffsetDateTime expiresAt,OffsetDateTime serverTime,Integer refreshAfterSeconds){}
 public record CheckinResult(boolean allowed,String reasonCode,String message,OffsetDateTime occurredAt,LocalDate visitDate){}
 public record HourCount(String hour,long granted,long denied){}
 public record Stats(LocalDate from,LocalDate to,String timezone,long total,long granted,long denied,List<HourCount> byHour){}
 public record KioskSession(String gymName,String slug,String deviceName,OffsetDateTime expiresAt,String csrfToken){}
 public record PublicConfig(String name,String slug,boolean checkinAvailable){}
}
