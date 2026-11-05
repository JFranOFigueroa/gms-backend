package com.gymback.shared.services;
import org.springframework.stereotype.Component;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
/** Solo escribe columnas existentes en la entidad, acorde al SQL real. */
@Component
public class AuditService {
  public void setAuditFields(Object entity, UUID actor, boolean isCreate) {
    OffsetDateTime now=OffsetDateTime.now(ZoneOffset.UTC);
    if(isCreate) { set(entity,"createdAt",now,true);set(entity,"createdBy",actor,true); }
    set(entity,"updatedAt",now,false);set(entity,"updatedBy",actor,false);
  }
  public void setDeleteFields(Object entity, UUID actor) {
    OffsetDateTime now=OffsetDateTime.now(ZoneOffset.UTC);
    set(entity,"deletedAt",now,false);set(entity,"deletedBy",actor,false);
    set(entity,"updatedAt",now,false);set(entity,"updatedBy",actor,false);
  }
  private void set(Object entity,String name,Object value,boolean onlyIfNull) {
    for(Class<?> c=entity.getClass(); c!=null;c=c.getSuperclass()) try {
      Field f=c.getDeclaredField(name);f.setAccessible(true);
      if(!onlyIfNull || f.get(entity)==null) f.set(entity,value);
      return;
    } catch(NoSuchFieldException ignored) {} catch(IllegalAccessException e) {throw new IllegalStateException(e);}
  }
}
