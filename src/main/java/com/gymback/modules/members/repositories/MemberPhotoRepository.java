package com.gymback.modules.members.repositories;

import com.gymback.modules.members.model.MemberPhoto;
import com.gymback.shared.repositories.BaseCRUD;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MemberPhotoRepository extends BaseCRUD<MemberPhoto> {
  public MemberPhotoRepository(EntityManager em) { super(em, MemberPhoto.class, true); }
  public Optional<MemberPhoto> activeForMember(UUID branchId, UUID memberId) {
    return em.createQuery(
        "select p from MemberPhoto p where p.branchId=:branch and p.memberId=:member and p.deletedAt is null",
        MemberPhoto.class)
      .setParameter("branch", branchId).setParameter("member", memberId)
      .setMaxResults(1).getResultStream().findFirst();
  }
}
