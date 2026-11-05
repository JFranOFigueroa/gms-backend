package com.gymback.shared.interfaces;
import com.gymback.shared.domain.BaseEntity;
import java.util.*;
public interface IBaseCrudRepository<T extends BaseEntity> {
  Optional<T> get(UUID id, UUID gymId);
  List<T> list(UUID gymId, int offset, int limit);
  /** Filtros tipados para operaciones especializadas, sin aceptar columnas del cliente. */
  List<T> getWhere(UUID gymId,org.springframework.data.jpa.domain.Specification<T> criteria,int offset,int limit);
  T create(T object);
  T update(T object);
  void delete(T object);
}
