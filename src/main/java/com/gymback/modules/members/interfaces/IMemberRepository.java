package com.gymback.modules.members.interfaces;
import com.gymback.shared.interfaces.IBaseCrudRepository;
import com.gymback.modules.members.model.BranchMember;
import java.util.*;
public interface IMemberRepository extends IBaseCrudRepository<BranchMember> {
  List<Map<String,Object>> listDetails(UUID branchId,int offset,int limit);
  Map<String,Object> detail(UUID branchId,UUID memberId);
  Map<String,Object> own(UUID branchId,UUID userId);
}
