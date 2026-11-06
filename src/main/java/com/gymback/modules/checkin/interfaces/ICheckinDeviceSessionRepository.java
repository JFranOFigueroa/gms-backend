package com.gymback.modules.checkin.interfaces;
import com.gymback.modules.checkin.model.CheckinDeviceSession;
import java.util.*;
public interface ICheckinDeviceSessionRepository {
 CheckinDeviceSession save(CheckinDeviceSession session);
 Optional<CheckinDeviceSession> byHash(String hash);
 void revokeForDevice(UUID device);
 void revoke(CheckinDeviceSession session);
 void touch(UUID sessionId,UUID deviceId,java.time.OffsetDateTime at);
}
