package com.gymback.modules.checkin.repositories;
import com.gymback.modules.checkin.model.CheckinDeviceSession;
import com.gymback.modules.checkin.interfaces.ICheckinDeviceSessionRepository;
import com.gymback.shared.repositories.BaseCRUD;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.*;
@Repository
public class CheckinDeviceSessionRepository extends BaseCRUD<CheckinDeviceSession> implements ICheckinDeviceSessionRepository {
 public CheckinDeviceSessionRepository(EntityManager em){super(em,CheckinDeviceSession.class,true);}
 public CheckinDeviceSession save(CheckinDeviceSession s){return em.contains(s)?update(s):create(s);}
 public Optional<CheckinDeviceSession> byHash(String hash){
  return em.createQuery("select s from CheckinDeviceSession s where s.sessionTokenHash=:hash",CheckinDeviceSession.class)
    .setParameter("hash",hash).setMaxResults(1).getResultStream().findFirst();
 }
 public void revokeForDevice(UUID device){
  em.createQuery("update CheckinDeviceSession s set s.revokedAt=:now where s.deviceId=:device and s.revokedAt is null and s.deletedAt is null")
    .setParameter("now",OffsetDateTime.now()).setParameter("device",device).executeUpdate();
 }
 @org.springframework.transaction.annotation.Transactional
 public void touch(UUID sessionId,UUID deviceId,OffsetDateTime at){
  // No generar dos eventos de auditoría en cada solicitud: la presencia es aproximada (5 min).
  OffsetDateTime threshold=at.minusMinutes(5);
  em.createQuery("update CheckinDeviceSession s set s.lastSeenAt=:at where s.id=:id and s.revokedAt is null and s.deletedAt is null and (s.lastSeenAt is null or s.lastSeenAt<:threshold)")
    .setParameter("at",at).setParameter("id",sessionId).setParameter("threshold",threshold).executeUpdate();
  em.createQuery("update CheckinDevice d set d.lastSeenAt=:at where d.id=:device and d.deletedAt is null and (d.lastSeenAt is null or d.lastSeenAt<:threshold)")
    .setParameter("at",at).setParameter("device",deviceId).setParameter("threshold",threshold).executeUpdate();
 }
 public void revoke(CheckinDeviceSession s){s.revokedAt=OffsetDateTime.now();em.flush();}
}
