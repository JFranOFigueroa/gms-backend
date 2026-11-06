package com.gymback.modules.checkin.repositories;

import com.gymback.modules.checkin.interfaces.ICheckinAccessCodeRepository;
import com.gymback.modules.checkin.model.CheckinAccessCode;
import com.gymback.shared.repositories.BaseCRUD;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;

@Repository
public class CheckinAccessCodeRepository extends BaseCRUD<CheckinAccessCode> implements ICheckinAccessCodeRepository {
 public CheckinAccessCodeRepository(EntityManager em){super(em,CheckinAccessCode.class,true);}
 public CheckinAccessCode save(CheckinAccessCode code){return em.contains(code)?update(code):create(code);}
 public Optional<CheckinAccessCode> byHash(UUID gym,String hash){
  return em.createQuery("select c from CheckinAccessCode c where c.gymId=:gym and c.codeHash=:hash",CheckinAccessCode.class)
   .setParameter("gym",gym).setParameter("hash",hash).setMaxResults(1).getResultStream().findFirst();
 }
 public Optional<CheckinAccessCode> lockByHash(UUID gym,String hash){
  Optional<CheckinAccessCode> code=em.createQuery("select c from CheckinAccessCode c where c.gymId=:gym and c.codeHash=:hash",CheckinAccessCode.class)
   .setParameter("gym",gym).setParameter("hash",hash).setLockMode(LockModeType.PESSIMISTIC_WRITE)
   .setMaxResults(1).getResultStream().findFirst();
  // Releer después de obtener el bloqueo: una emisión paralela pudo revocarlo.
  code.ifPresent(c->em.refresh(c,LockModeType.PESSIMISTIC_WRITE));
  return code;
 }
 public long issuedSince(UUID gym,UUID member,OffsetDateTime since){
  return em.createQuery("select count(c) from CheckinAccessCode c where c.gymId=:gym and c.memberId=:member and c.issuedAt>=:since",Long.class)
   .setParameter("gym",gym).setParameter("member",member).setParameter("since",since).getSingleResult();
 }
 public void revokeOutstanding(UUID gym,UUID member,OffsetDateTime at){
  em.createQuery("update CheckinAccessCode c set c.revokedAt=:at where c.gymId=:gym and c.memberId=:member and c.consumedAt is null and c.revokedAt is null and c.deletedAt is null")
   .setParameter("at",at).setParameter("gym",gym).setParameter("member",member).executeUpdate();
 }
}
