package com.gymback.modules.checkin.interfaces;
import com.gymback.modules.checkin.model.*;
import java.util.*;
public interface ICheckinDeviceRepository {
 List<CheckinDevice> list(UUID branch);
 Optional<CheckinDevice> findInGym(UUID branch,UUID id);
 Optional<CheckinDevice> lockInGym(UUID branch,UUID id);
 Optional<CheckinDevice> byActivationHash(String hash,boolean lock);
 Optional<CheckinDevice> byName(UUID branch,String name);
 CheckinDevice save(CheckinDevice device);
}
