package com.gymback.shared.repositories;
import com.gymback.shared.domain.BaseEntity;
import com.gymback.shared.interfaces.IBaseCrudRepository;
import jakarta.persistence.*;
import jakarta.persistence.criteria.*;
import java.lang.reflect.Field;
import java.util.*;
/** Equivalente a BaseCRUD de Python. Nunca permite búsqueda sin gymId en entidades tenant. */
public abstract class BaseCRUD<T extends BaseEntity> implements IBaseCrudRepository<T> {
  protected final EntityManager em;
  protected final Class<T> model;
  private final boolean tenantScoped;
  protected BaseCRUD(EntityManager em, Class<T> model, boolean tenantScoped) {
    this.em=em; this.model=model; this.tenantScoped=tenantScoped;
  }
  private void requireScope(UUID gymId) {
    if (tenantScoped && gymId==null) throw new IllegalArgumentException("gymId obligatorio");
  }
  @Override public Optional<T> get(UUID id, UUID gymId) {
    requireScope(gymId);
    T object=em.find(model,id);
    if (object==null || (tenantScoped && !gymId.equals(object.getGymId())) || isDeleted(object)) return Optional.empty();
    return Optional.of(object);
  }
  @Override public List<T> list(UUID gymId,int offset,int limit) { return getWhere(gymId,null,offset,limit); }
  @Override public List<T> getWhere(UUID gymId,org.springframework.data.jpa.domain.Specification<T> criteria,int offset,int limit){
    requireScope(gymId);
    if(offset<0 || limit<1 || limit>100)throw new IllegalArgumentException("Paginación inválida (limit 1..100)");
    CriteriaBuilder criteriaBuilder=em.getCriteriaBuilder();
    CriteriaQuery<T> query=criteriaBuilder.createQuery(model);
    Root<T> root=query.from(model);
    java.util.List<Predicate> conditions=new java.util.ArrayList<>();
    if(tenantScoped)conditions.add(criteriaBuilder.equal(root.get("gymId"),gymId));
    if(hasDeletedAt())conditions.add(criteriaBuilder.isNull(root.get("deletedAt")));
    if(criteria!=null){Predicate predicate=criteria.toPredicate(root,query,criteriaBuilder);if(predicate!=null)conditions.add(predicate);}
    query.select(root).where(conditions.toArray(new Predicate[0])).orderBy(criteriaBuilder.asc(root.get("id")));
    return em.createQuery(query).setFirstResult(offset).setMaxResults(limit).getResultList();
  }
  @Override public T create(T object) { em.persist(object);em.flush();return object; }
  @Override public T update(T object) { T managed=em.merge(object);em.flush();return managed; }
  /** Nunca em.remove: borrado lógico uniforme para todos los modelos. */
  @Override public void delete(T object) {
    if (!hasDeletedAt()) throw new IllegalStateException("Modelo sin deletedAt: " + model.getName());
    for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass()) try {
      Field f=c.getDeclaredField("deletedAt");f.setAccessible(true);
      if(f.get(object)!=null) return;
      f.set(object,java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
      em.merge(object);em.flush();return;
    } catch(NoSuchFieldException ignored){} catch(IllegalAccessException e){throw new IllegalStateException(e);}
    throw new IllegalStateException("No existe deletedAt");
  }
  protected boolean hasDeletedAt() {
    for(Class<?> c=model;c!=null;c=c.getSuperclass()) try {c.getDeclaredField("deletedAt"); return true;}catch(NoSuchFieldException ignored){}
    return false;
  }
  private boolean isDeleted(T object) {
    for(Class<?> c=object.getClass();c!=null;c=c.getSuperclass()) try{
      Field f=c.getDeclaredField("deletedAt");f.setAccessible(true);return f.get(object)!=null;
    } catch(NoSuchFieldException ignored){} catch(IllegalAccessException e){throw new IllegalStateException(e);}
    return false;
  }
}
