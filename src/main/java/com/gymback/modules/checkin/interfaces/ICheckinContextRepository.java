package com.gymback.modules.checkin.interfaces;
import java.util.*;
public interface ICheckinContextRepository {
 record BranchContext(UUID id,String name,String code,String timezone,int qrTtl){}
 Optional<BranchContext> byCode(String code);
 Optional<BranchContext> byId(UUID id);
}
