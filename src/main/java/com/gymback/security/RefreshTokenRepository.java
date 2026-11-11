package com.gymback.security;
import com.gymback.shared.persistence.model.UserSession;
import jakarta.persistence.*;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.*;
@Repository
public class RefreshTokenRepository implements IRefreshTokenRepository {
  private final EntityManager em;
  public RefreshTokenRepository(EntityManager em){this.em=em;}
  @Override public Optional<Session> lockValid(String hash,Instant now){
    return em.createQuery("select s from UserSession s where s.refreshTokenHash=:hash and s.revokedAt is null and s.expiresAt>:now",UserSession.class)
      .setParameter("hash",hash).setParameter("now",now.atOffset(ZoneOffset.UTC))
      .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultStream()
      .map(s->new Session(s.id,s.userId,s.expiresAt.toInstant())).findFirst();
  }
  @Override public void insert(UUID userId,String hash,Instant expiresAt){
    insert(UUID.randomUUID(),userId,hash,expiresAt);
  }
  @Override public void insert(UUID id,UUID userId,String hash,Instant expiresAt){
    UserSession s=new UserSession();s.id=id;s.userId=userId;s.refreshTokenHash=hash;s.expiresAt=expiresAt.atOffset(ZoneOffset.UTC);
    em.persist(s);em.flush();
  }
  @Override @org.springframework.transaction.annotation.Transactional(readOnly=true)
  public boolean isActive(UUID sessionId,UUID userId,Instant now){
    return !em.createQuery("select s.id from UserSession s where s.id=:id and s.userId=:user and s.revokedAt is null and s.expiresAt>:now",UUID.class)
      .setParameter("id",sessionId).setParameter("user",userId).setParameter("now",now.atOffset(ZoneOffset.UTC))
      .setMaxResults(1).getResultList().isEmpty();
  }
  @Override public void revoke(UUID id){
    UserSession s=em.find(UserSession.class,id);
    if(s!=null && s.revokedAt==null){s.revokedAt=OffsetDateTime.now(ZoneOffset.UTC);s.deletedAt=s.revokedAt;em.flush();}
  }
  @Override public void revokeHash(String hash){
    // No reconsulta tokens de sesión revocados: idempotente ante logout duplicado.
    em.createQuery("update UserSession s set s.revokedAt=:now,s.deletedAt=:now where s.refreshTokenHash=:hash and s.revokedAt is null and s.deletedAt is null")
      .setParameter("now",OffsetDateTime.now(ZoneOffset.UTC)).setParameter("hash",hash).executeUpdate();
  }
}
