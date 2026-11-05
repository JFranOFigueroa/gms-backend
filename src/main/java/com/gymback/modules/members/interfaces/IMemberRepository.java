package com.gymback.modules.members.interfaces;
import com.gymback.shared.interfaces.IBaseCrudRepository;
import com.gymback.modules.members.model.GymMember;
import java.util.*;
public interface IMemberRepository extends IBaseCrudRepository<GymMember> {
  List<Map<String,Object>> listDetails(UUID gymId,int offset,int limit);
  Map<String,Object> detail(UUID gymId,UUID memberId);
  Map<String,Object> own(UUID gymId,UUID userId);
}
