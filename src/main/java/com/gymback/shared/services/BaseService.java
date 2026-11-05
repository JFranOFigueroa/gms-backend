package com.gymback.shared.services;
import com.gymback.shared.domain.BaseEntity;
import com.gymback.shared.interfaces.IBaseCrudRepository;
import com.gymback.shared.exceptions.NotFoundException;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
/** Equivalente al BaseService de FastAPI. No incluye reglas de negocio de cada módulo. */
public abstract class BaseService<T extends BaseEntity> {
  protected final IBaseCrudRepository<T> repo;
  protected final AuditService audit;
  protected BaseService(IBaseCrudRepository<T> repo, AuditService audit) {this.repo=repo;this.audit=audit;}
  @Transactional(readOnly=true) public T getOr404(UUID id,UUID gymId) { return repo.get(id,gymId).orElseThrow(()->new NotFoundException("Registro no encontrado")); }
  @Transactional(readOnly=true) public Optional<T> getOrNone(UUID id, UUID gymId) {return repo.get(id,gymId);}
  @Transactional(readOnly=true) public List<T> list(UUID gymId,int offset,int limit) {return repo.list(gymId,offset,limit);}
  @Transactional(readOnly=true) protected void checkExists(UUID id,UUID gymId,String message){
    if(repo.get(id,gymId).isPresent())throw new com.gymback.shared.exceptions.DomainException(message);
  }
  @Transactional protected T create(T entity,UUID actor) {audit.setAuditFields(entity,actor,true);return repo.create(entity);}
  @Transactional protected T update(T entity,UUID actor) {audit.setAuditFields(entity,actor,false);return repo.update(entity);}
  @Deprecated @Transactional protected void hardDelete(T entity) {throw new IllegalStateException("Usar softDelete(entity,actor)");}
  @Transactional protected void softDelete(T entity,UUID actor) {audit.setDeleteFields(entity,actor);repo.update(entity);}
}
