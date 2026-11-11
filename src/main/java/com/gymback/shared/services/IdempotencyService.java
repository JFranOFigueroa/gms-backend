package com.gymback.shared.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymback.shared.exceptions.ConflictException;
import com.gymback.shared.exceptions.DomainException;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** PostgreSQL-backed, transaction-scoped idempotency for money/stock operations. */
@Service
public class IdempotencyService {
  private final EntityManager em;
  private final ObjectMapper mapper;
  private final String table;
  public IdempotencyService(EntityManager em,ObjectMapper mapper,
    @Value("${schema_db:${SCHEMA_DB:test}}") String schema){
    if(schema==null || !schema.matches("[A-Za-z_][A-Za-z0-9_]*"))
      throw new IllegalArgumentException("Nombre de esquema PostgreSQL inválido");
    this.em=em;this.mapper=mapper;
    // WildFly/varios perfiles no garantizan search_path para consultas nativas.
    this.table="\""+schema+"\".request_idempotency";
  }

  public record Claim(UUID gymId,UUID actorId,String operation,String key,UUID existingResourceId) {
    public boolean replay(){return existingResourceId!=null;}
  }

  public Claim claim(UUID gym,UUID actor,String operation,String suppliedKey,Object request){
    if(suppliedKey==null)return null;
    String key=suppliedKey.strip();
    if(!key.matches("[A-Za-z0-9._:-]{8,128}"))
      throw new DomainException("Idempotency-Key debe contener entre 8 y 128 caracteres seguros");
    String fingerprint=fingerprint(request);
    // Unique constraint serializes simultaneous duplicate requests across all JVM instances.
    int inserted=em.createNativeQuery("""
      INSERT INTO %s(id,gym_id,actor_id,operation,idempotency_key,request_hash)
      VALUES (:id,:gym,:actor,:operation,:key,:hash)
      ON CONFLICT (gym_id,actor_id,operation,idempotency_key) DO NOTHING
      """.formatted(table))
      .setParameter("id",UUID.randomUUID()).setParameter("gym",gym).setParameter("actor",actor)
      .setParameter("operation",operation).setParameter("key",key).setParameter("hash",fingerprint)
      .executeUpdate();
    if(inserted==1)return new Claim(gym,actor,operation,key,null);

    @SuppressWarnings("unchecked")
    List<Object[]> rows=em.createNativeQuery("""
      SELECT request_hash,resource_id FROM %s
      WHERE gym_id=:gym AND actor_id=:actor AND operation=:operation AND idempotency_key=:key
      FOR UPDATE
      """.formatted(table))
      .setParameter("gym",gym).setParameter("actor",actor)
      .setParameter("operation",operation).setParameter("key",key).getResultList();
    if(rows.size()!=1)throw new IllegalStateException("Solicitud idempotente no encontrada");
    if(!fingerprint.equals(rows.get(0)[0]))
      throw new ConflictException("idempotency_key_reused","Esta clave ya se usó con otros datos");
    if(rows.get(0)[1]==null)throw new IllegalStateException("Solicitud idempotente incompleta");
    return new Claim(gym,actor,operation,key,(UUID)rows.get(0)[1]);
  }

  public void complete(Claim claim,UUID resourceId){
    if(claim==null)return;
    if(claim.replay())throw new IllegalStateException("No puede completarse un reintento");
    int updated=em.createNativeQuery("""
      UPDATE %s SET resource_id=:resource
      WHERE gym_id=:gym AND actor_id=:actor AND operation=:operation
        AND idempotency_key=:key AND resource_id IS NULL
      """.formatted(table))
      .setParameter("resource",resourceId).setParameter("gym",claim.gymId())
      .setParameter("actor",claim.actorId()).setParameter("operation",claim.operation())
      .setParameter("key",claim.key()).executeUpdate();
    if(updated!=1)throw new IllegalStateException("No se pudo completar la solicitud idempotente");
  }

  private String fingerprint(Object payload){
    try{
      byte[] bytes=mapper.writeValueAsBytes(payload);
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }catch(JsonProcessingException | java.security.NoSuchAlgorithmException ex){throw new IllegalStateException("No se pudo calcular huella de solicitud",ex);}
  }
}
