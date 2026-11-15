package com.gymback.modules.branches.repositories;
import com.gymback.shared.repositories.BaseCRUD;
import com.gymback.modules.branches.model.Branch;
import com.gymback.modules.branches.interfaces.IBranchRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.UUID;
@Repository
public class BranchRepository extends BaseCRUD<Branch> implements IBranchRepository {
  public BranchRepository(EntityManager em){super(em,Branch.class,false);}
  @Override public boolean codeExists(String code,UUID exceptBranchId){
    // La restricción unique de CITEXT también cubre los codes de sucursals archivados.
    return em.createQuery("select count(g) from BranchCodeRegistry g where lower(g.code)=lower(:code) and (:except is null or g.id<>:except)",Long.class)
      .setParameter("code",code).setParameter("except",exceptBranchId).getSingleResult()>0;
  }
}
