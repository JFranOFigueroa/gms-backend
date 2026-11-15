package com.gymback.modules.branches.interfaces;
import com.gymback.shared.interfaces.IBaseCrudRepository;
import com.gymback.modules.branches.model.Branch;
public interface IBranchRepository extends IBaseCrudRepository<Branch> {
  boolean codeExists(String code,java.util.UUID exceptBranchId);
}
