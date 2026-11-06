package com.gymback.modules.checkin.interfaces;

import com.gymback.modules.checkin.model.CheckinAccessCode;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ICheckinAccessCodeRepository {
 CheckinAccessCode save(CheckinAccessCode code);
 Optional<CheckinAccessCode> byHash(UUID gym,String codeHash);
 Optional<CheckinAccessCode> lockByHash(UUID gym,String codeHash);
 long issuedSince(UUID gym,UUID member,OffsetDateTime since);
 void revokeOutstanding(UUID gym,UUID member,OffsetDateTime at);
}
