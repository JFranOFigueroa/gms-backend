package com.gymback.modules.branches.interfaces;
import com.gymback.shared.interfaces.IBaseCrudRepository;
import com.gymback.modules.branches.model.Branch;
import java.util.List;
public interface IBranchRepository extends IBaseCrudRepository<Branch> {
  boolean codeExists(String code,java.util.UUID exceptBranchId);
  /** Sucursales activas para el selector público de check-in. */
  List<Branch> active();
}
