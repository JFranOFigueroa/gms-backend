package com.gymback.modules.checkin.interfaces;

import com.gymback.modules.checkin.model.CheckinAccessCode;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ICheckinAccessCodeRepository {
 CheckinAccessCode save(CheckinAccessCode code);
 Optional<CheckinAccessCode> byHash(UUID branch,String codeHash);
 Optional<CheckinAccessCode> lockByHash(UUID branch,String codeHash);
 long issuedSince(UUID branch,UUID member,OffsetDateTime since);
 void revokeOutstanding(UUID branch,UUID member,OffsetDateTime at);
}
