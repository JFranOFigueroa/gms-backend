package com.gymback.modules.checkin.interfaces;
import com.gymback.modules.checkin.model.*;
import java.util.*;
public interface ICheckinDeviceRepository {
 List<CheckinDevice> list(UUID gym);
 Optional<CheckinDevice> findInGym(UUID gym,UUID id);
 Optional<CheckinDevice> lockInGym(UUID gym,UUID id);
 Optional<CheckinDevice> byActivationHash(String hash,boolean lock);
 Optional<CheckinDevice> byName(UUID gym,String name);
 CheckinDevice save(CheckinDevice device);
}
