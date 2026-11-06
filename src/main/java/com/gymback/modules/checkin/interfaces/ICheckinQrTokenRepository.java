package com.gymback.modules.checkin.interfaces;
import com.gymback.modules.checkin.model.CheckinQrToken;
import java.util.*;
public interface ICheckinQrTokenRepository {
 CheckinQrToken save(CheckinQrToken token);
 Optional<CheckinQrToken> byHash(String hash);
 void revokeForDevice(UUID device);
 long countIssuedSince(UUID device,java.time.OffsetDateTime since);
}
