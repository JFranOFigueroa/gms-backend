package com.gymback.modules.calendar.interfaces;
import com.gymback.shared.interfaces.IBaseCrudRepository;
import com.gymback.modules.calendar.model.BranchClass;
public interface IBranchClassRepository extends IBaseCrudRepository<BranchClass> {
 java.util.List<BranchClass> calendar(java.util.UUID branchId,java.time.LocalDate from,java.time.LocalDate to,String status,int offset,int limit);
}
