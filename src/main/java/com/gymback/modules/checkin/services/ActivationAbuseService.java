package com.gymback.modules.checkin.services;
import com.gymback.modules.checkin.model.CheckinActivationAttempt;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.gymback.shared.exceptions.RateLimitException;
import java.time.OffsetDateTime;
/** Persistencia en BD, no mapa de una JVM; registrar fallos fuera de la transacción de activación. */
@Service
public class ActivationAbuseService {
 private final EntityManager em;
 public ActivationAbuseService(EntityManager em){this.em=em;}
 @Transactional(readOnly=true)public void check(String clientHash){
  long count=em.createQuery("select count(a) from CheckinActivationAttempt a where a.clientHash=:client and a.triedAt>=:from",Long.class)
   .setParameter("client",clientHash).setParameter("from",OffsetDateTime.now().minusMinutes(15)).getSingleResult();
  if(count>=5)throw new RateLimitException(900);
 }
 @Transactional(propagation=Propagation.REQUIRES_NEW)public void recordFailure(String clientHash){
  CheckinActivationAttempt attempt=new CheckinActivationAttempt();attempt.clientHash=clientHash;em.persist(attempt);
 }
}
