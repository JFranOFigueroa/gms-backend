package com.gymback.modules.calendar.services;
import com.gymback.modules.calendar.model.GymClass;
import com.gymback.modules.calendar.interfaces.IGymClassRepository;
import com.gymback.modules.calendar.dto.GymClassRequest;
import com.gymback.shared.services.*;
import com.gymback.modules.calendar.model.ClassCategories;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.security.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.LocalDate;
public class GymClassService extends BaseService<GymClass> {
  private final AuthorizationService auth;
  public GymClassService(IGymClassRepository repo,AuditService audit,AuthorizationService auth) {super(repo,audit);this.auth=auth;}
  @Transactional(readOnly=true) public List<GymClass> all(UUID gymId,int offset,int limit){ auth.requireModule(gymId,"calendar","owner","trainer","member"); return list(gymId,offset,limit); }
  @Transactional(readOnly=true) public List<GymClass> calendar(UUID gym,int offset,int limit,LocalDate from,LocalDate to,String status){
    auth.requireModule(gym,"calendar","owner","trainer","member");
    if(offset<0||limit<1||limit>100||from!=null&&to!=null&&from.isAfter(to))throw new DomainException("Rango/paginación inválidos");
    String state=status==null||status.isBlank()?"scheduled":status;
    if(!Set.of("scheduled","cancelled","completed","all").contains(state))throw new DomainException("Estado inválido");
    return ((IGymClassRepository)repo).calendar(gym,from,to,state,offset,limit);
  }
  @Transactional(readOnly=true) public GymClass one(UUID gymId,UUID id){ auth.requireModule(gymId,"calendar","owner","trainer","member"); return getOr404(id,gymId); }
  @Transactional public GymClass add(UUID gymId,GymClassRequest req){
    Actor actor=auth.requireModule(gymId,"calendar","owner","trainer");
    if(req.name()==null || req.name().isBlank()) throw new DomainException("name es obligatorio");if(req.startsAt()==null) throw new DomainException("startsAt es obligatorio");if(req.endsAt()==null) throw new DomainException("endsAt es obligatorio");if(req.capacity()==null) throw new DomainException("capacity es obligatorio");
    GymClass e=new GymClass();
    e.setGymId(gymId);
    if(req.templateId()!=null) e.setTemplateId(req.templateId()); if(req.name()!=null) e.setName(req.name()); if(req.category()!=null) e.setCategory(req.category()); if(req.color()!=null) e.setColor(req.color()); if(req.startsAt()!=null) e.setStartsAt(req.startsAt()); if(req.endsAt()!=null) e.setEndsAt(req.endsAt()); if(req.capacity()!=null) e.setCapacity(req.capacity()); if(req.trainerUserId()!=null) e.setTrainerUserId(req.trainerUserId()); if(req.status()!=null) e.setStatus(req.status());
    e.setCreatedBy(actor.id());
    validate(e);
    return create(e,actor.id());
  }
  @Transactional public GymClass change(UUID gymId,UUID id,GymClassRequest req){
    Actor actor=auth.requireModule(gymId,"calendar","owner","trainer");
    GymClass e=getOr404(id,gymId);
    if(req.templateId()!=null) e.setTemplateId(req.templateId()); if(req.name()!=null) e.setName(req.name()); if(req.category()!=null) e.setCategory(req.category()); if(req.color()!=null) e.setColor(req.color()); if(req.startsAt()!=null) e.setStartsAt(req.startsAt()); if(req.endsAt()!=null) e.setEndsAt(req.endsAt()); if(req.capacity()!=null) e.setCapacity(req.capacity()); if(req.trainerUserId()!=null) e.setTrainerUserId(req.trainerUserId()); if(req.status()!=null) e.setStatus(req.status());
    validate(e);
    return update(e,actor.id());
  }
  @Transactional public void remove(UUID gymId,UUID id){
    Actor actor=auth.requireModule(gymId,"calendar","owner","trainer");
    GymClass e=getOr404(id,gymId);
    e.setStatus("cancelled");softDelete(e,actor.id());
  }
  private void validate(GymClass e){ if(e.getEndsAt()==null || e.getStartsAt()==null || !e.getEndsAt().isAfter(e.getStartsAt()))throw new DomainException("Rango horario inválido");
  if(e.getCapacity()==null || e.getCapacity()<1)throw new DomainException("Capacidad inválida");
  if(!ClassCategories.allowed(e.getCategory()))throw new DomainException("Categoría inválida"); }
}
