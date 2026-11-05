package com.gymback.modules.calendar.services;
import com.gymback.modules.calendar.model.ClassTemplate;
import com.gymback.modules.calendar.interfaces.IClassTemplateRepository;
import com.gymback.modules.calendar.dto.ClassTemplateRequest;
import com.gymback.shared.services.*;
import com.gymback.modules.calendar.model.ClassCategories;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.security.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class ClassTemplateService extends BaseService<ClassTemplate> {
  private final AuthorizationService auth;
  public ClassTemplateService(IClassTemplateRepository repo,AuditService audit,AuthorizationService auth) {super(repo,audit);this.auth=auth;}
  @Transactional(readOnly=true) public List<ClassTemplate> all(UUID gymId,int offset,int limit){ auth.requireModule(gymId,"calendar","owner","trainer","member"); return list(gymId,offset,limit); }
  @Transactional(readOnly=true) public ClassTemplate one(UUID gymId,UUID id){ auth.requireModule(gymId,"calendar","owner","trainer","member"); return getOr404(id,gymId); }
  @Transactional public ClassTemplate add(UUID gymId,ClassTemplateRequest req){
    Actor actor=auth.requireModule(gymId,"calendar","owner","trainer");
    if(req.name()==null || req.name().isBlank()) throw new DomainException("name es obligatorio");if(req.capacity()==null) throw new DomainException("capacity es obligatorio");
    ClassTemplate e=new ClassTemplate();
    e.setGymId(gymId);
    if(req.name()!=null) e.setName(req.name()); if(req.category()!=null) e.setCategory(req.category()); if(req.color()!=null) e.setColor(req.color()); if(req.durationMinutes()!=null) e.setDurationMinutes(req.durationMinutes()); if(req.capacity()!=null) e.setCapacity(req.capacity()); if(req.isActive()!=null) e.setIsActive(req.isActive());

    validate(e);
    return create(e,actor.id());
  }
  @Transactional public ClassTemplate change(UUID gymId,UUID id,ClassTemplateRequest req){
    Actor actor=auth.requireModule(gymId,"calendar","owner","trainer");
    ClassTemplate e=getOr404(id,gymId);
    if(req.name()!=null) e.setName(req.name()); if(req.category()!=null) e.setCategory(req.category()); if(req.color()!=null) e.setColor(req.color()); if(req.durationMinutes()!=null) e.setDurationMinutes(req.durationMinutes()); if(req.capacity()!=null) e.setCapacity(req.capacity()); if(req.isActive()!=null) e.setIsActive(req.isActive());
    validate(e);
    return update(e,actor.id());
  }
  @Transactional public void remove(UUID gymId,UUID id){
    Actor actor=auth.requireModule(gymId,"calendar","owner","trainer");
    ClassTemplate e=getOr404(id,gymId);
    e.setIsActive(false);softDelete(e,actor.id());
  }
  private void validate(ClassTemplate e){ if(e.getCapacity()==null || e.getCapacity()<1 || e.getDurationMinutes()==null || e.getDurationMinutes()<1)throw new DomainException("Capacidad/duración inválida");
  if(!ClassCategories.allowed(e.getCategory()))throw new DomainException("Categoría no permitida por SQL"); }
}
