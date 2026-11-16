package com.gymback.modules.routines.services;
import com.gymback.modules.routines.model.Routine;
import com.gymback.modules.routines.interfaces.*;
import com.gymback.modules.routines.dto.*;
import com.gymback.shared.services.*;
import com.gymback.shared.exceptions.*;
import com.gymback.security.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

public class RoutineService extends BaseService<Routine> {
  private final AuthorizationService auth;
  private final IRoutineStructureRepository structure;
  private final IRoutineRepository routines;
  public RoutineService(IRoutineRepository repo,AuditService audit,AuthorizationService auth,
                        IRoutineStructureRepository structure) {
    super(repo,audit);this.auth=auth;this.structure=structure;this.routines=repo;
  }
  private boolean mayViewDrafts(UUID branch, Actor actor) {
    return actor.platformAdmin() || auth.hasRole(branch,"owner") || auth.hasRole(branch,"trainer");
  }
  @Transactional(readOnly=true)
  public List<RoutineView> all(UUID branch,int offset,int limit) {
    Actor actor=auth.require(branch,"owner","trainer","member");
    // Filtrar ANTES de paginar: no traer borradores desde la base para un miembro.
    List<Routine> visible=mayViewDrafts(branch,actor)
      ? routines.list(branch,offset,limit)
      : routines.getWhere(branch,(root,query,cb)->cb.isTrue(root.get("published")),offset,limit);
    return visible.stream().map(r->view(branch,r)).toList();
  }
  @Transactional(readOnly=true)
  public RoutineView one(UUID branch,UUID id) {
    Actor actor=auth.require(branch,"owner","trainer","member");
    Routine r=getOr404(id,branch);
    if (!mayViewDrafts(branch,actor) && !Boolean.TRUE.equals(r.getPublished())) {
      throw new NotFoundException("Rutina no encontrada");
    }
    return view(branch,r);
  }
  private RoutineView view(UUID branch,Routine r) {return RoutineView.of(r,structure.list(branch,r.getId()));}
  @Transactional
  public RoutineView add(UUID branch,RoutineRequest d) {
    Actor actor=auth.require(branch,"owner","trainer");
    if(d==null || d.name()==null || d.name().isBlank()) throw new DomainException("Nombre obligatorio");
    Routine r=new Routine();r.setBranchId(branch);apply(r,d);r.setCreatedBy(actor.id());validate(r);create(r,actor.id());
    if(d.days()!=null)structure.replace(branch,r.getId(),d.days());
    return view(branch,r);
  }
  @Transactional
  public RoutineView change(UUID branch,UUID id,RoutineRequest d) {
    Actor actor=auth.require(branch,"owner","trainer");
    if(d==null) throw new DomainException("Datos obligatorios");
    Routine r=getOr404(id,branch);apply(r,d);validate(r);update(r,actor.id());
    if(d.days()!=null)structure.replace(branch,id,d.days());
    return view(branch,r);
  }
  private void apply(Routine r,RoutineRequest d) {
    if(d.name()!=null)r.setName(d.name());if(d.description()!=null)r.setDescription(d.description());
    if(d.level()!=null)r.setLevel(d.level());if(d.goal()!=null)r.setGoal(d.goal());
    if(d.published()!=null)r.setPublished(d.published());
  }
  private void validate(Routine r) {
    if(!Set.of("beginner","intermediate","advanced").contains(r.getLevel()))
      throw new DomainException("Nivel inválido");
  }
  @Transactional
  public void remove(UUID branch,UUID id) {Actor actor=auth.require(branch,"owner","trainer");
    Routine target=getOr404(id,branch);structure.softDelete(branch,id);softDelete(target,actor.id());}
}
