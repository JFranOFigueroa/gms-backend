package com.gymback.modules.checkin.repositories;
import com.gymback.modules.checkin.model.CheckinQrToken;
import com.gymback.modules.checkin.interfaces.ICheckinQrTokenRepository;
import com.gymback.shared.repositories.BaseCRUD;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;
@Repository
public class CheckinQrTokenRepository extends BaseCRUD<CheckinQrToken> implements ICheckinQrTokenRepository {
 public CheckinQrTokenRepository(EntityManager em){super(em,CheckinQrToken.class,true);}
 public CheckinQrToken save(CheckinQrToken t){return em.contains(t)?update(t):create(t);}
 public Optional<CheckinQrToken> byHash(String hash){
  return em.createQuery("select t from CheckinQrToken t where t.tokenHash=:hash",CheckinQrToken.class)
    .setParameter("hash",hash).setMaxResults(1).getResultStream().findFirst();
 }
 public long countIssuedSince(UUID device,OffsetDateTime since){
  return em.createQuery("select count(t) from CheckinQrToken t where t.deviceId=:device and t.issuedAt>=:since",Long.class)
   .setParameter("device",device).setParameter("since",since).getSingleResult();
 }
 public void revokeForDevice(UUID device){
  em.createQuery("update CheckinQrToken t set t.deletedAt=:now where t.deviceId=:device and t.deletedAt is null")
    .setParameter("now",OffsetDateTime.now()).setParameter("device",device).executeUpdate();
 }
}
