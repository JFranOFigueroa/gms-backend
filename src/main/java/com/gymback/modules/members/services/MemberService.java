package com.gymback.modules.members.services;
import com.gymback.modules.members.dto.MemberRequest;
import com.gymback.modules.members.model.GymMember;
import com.gymback.modules.members.interfaces.IMemberRepository;
import com.gymback.shared.services.*;
import com.gymback.shared.exceptions.*;
import com.gymback.security.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

public class MemberService extends BaseService<GymMember> {
  private final AuthorizationService auth;
  private final IIdentityRepository accounts;
  private final IMemberRepository members;
  public MemberService(IMemberRepository repo, AuditService audit, AuthorizationService auth, IIdentityRepository accounts) {
    super(repo,audit);this.auth=auth;this.accounts=accounts;this.members=repo;
  }
  @Transactional(readOnly=true)
  public List<Map<String,Object>> all(UUID gym,int offset,int limit) {
    auth.require(gym,"owner","trainer");return members.listDetails(gym,offset,limit);
  }
  @Transactional(readOnly=true)
  public Map<String,Object> one(UUID gym,UUID id) {
    auth.require(gym,"owner","trainer");getOr404(id,gym);return members.detail(gym,id);
  }
  @Transactional(readOnly=true)
  public Map<String,Object> own(UUID gym){
    Actor actor=auth.require(gym,"member");
    return Optional.ofNullable(members.own(gym,actor.id())).orElseThrow(()->new NotFoundException("Expediente propio no encontrado"));
  }
  @Transactional
  public Map<String,Object> add(UUID gym,MemberRequest data) {
    auth.require(gym,"owner");
    if (data==null || data.userId()==null) throw new DomainException("userId de un usuario existente es obligatorio");
    var user=accounts.findCredentialsById(data.userId())
      .orElseThrow(()->new DomainException("Usuario global no encontrado"));
    if (!"active".equals(user.status())) throw new DomainException("Solo se vinculan usuarios globales activos");
    GymMember m=new GymMember();m.gymId=gym;m.userId=data.userId();
    m.memberCode=data.code()==null || data.code().isBlank()
      ? "M-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT) : data.code();
    apply(m,data);
    create(m,auth.actor().id());
    accounts.setDefaultGymIfMissing(data.userId(),gym);
    return members.detail(gym,m.id);
  }
  @Transactional
  public Map<String,Object> change(UUID gym,UUID id,MemberRequest data) {
    auth.require(gym,"owner");
    if (data==null) throw new DomainException("Datos obligatorios");
    if (data.userId()!=null) throw new DomainException("No se permite cambiar userId en un expediente");
    GymMember m=getOr404(id,gym);
    if (data.code()!=null) {
      if (data.code().isBlank()) throw new DomainException("Código de miembro vacío");
      m.memberCode=data.code();
    }
    apply(m,data);update(m,auth.actor().id());return members.detail(gym,id);
  }
  private void apply(GymMember m,MemberRequest d) {
    if(d.birthDate()!=null)m.birthDate=d.birthDate();if(d.address()!=null)m.address=d.address();
    if(d.emergencyContactName()!=null)m.emergencyContactName=d.emergencyContactName();
    if(d.emergencyContactPhone()!=null)m.emergencyContactPhone=d.emergencyContactPhone();
    if(d.photoAssetId()!=null)m.photoAssetId=d.photoAssetId();if(d.notes()!=null)m.notes=d.notes();
    if(d.startDate()!=null)m.joinedOn=d.startDate();if(d.active()!=null)m.isActive=d.active();
  }
  @Transactional
  public void remove(UUID gym,UUID id) {
    auth.require(gym,"owner");GymMember m=getOr404(id,gym);m.isActive=false;softDelete(m,auth.actor().id());
  }
}
