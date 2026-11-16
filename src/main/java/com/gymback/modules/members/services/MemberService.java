package com.gymback.modules.members.services;
import com.gymback.modules.members.dto.MemberRequest;
import com.gymback.modules.members.model.BranchMember;
import com.gymback.modules.members.model.MemberPhoto;
import com.gymback.modules.members.interfaces.IMemberRepository;
import com.gymback.modules.members.repositories.MemberPhotoRepository;
import com.gymback.modules.files.services.LocalImageStorage;
import com.gymback.shared.services.*;
import com.gymback.shared.exceptions.*;
import com.gymback.security.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

public class MemberService extends BaseService<BranchMember> {
  private final AuthorizationService auth;
  private final IIdentityRepository accounts;
  private final IMemberRepository members;
  private final MemberPhotoRepository photos;
  private final LocalImageStorage files;
  public MemberService(IMemberRepository repo, AuditService audit, AuthorizationService auth,
      IIdentityRepository accounts, MemberPhotoRepository photos, LocalImageStorage files) {
    super(repo,audit);this.auth=auth;this.accounts=accounts;this.members=repo;
    this.photos=photos;this.files=files;
  }
  @Transactional(readOnly=true)
  public List<Map<String,Object>> all(UUID branch,int offset,int limit) {
    auth.require(branch,"owner","trainer");return members.listDetails(branch,offset,limit);
  }
  @Transactional(readOnly=true)
  public Map<String,Object> one(UUID branch,UUID id) {
    auth.require(branch,"owner","trainer");getOr404(id,branch);return members.detail(branch,id);
  }
  @Transactional(readOnly=true)
  public Map<String,Object> own(UUID branch){
    Actor actor=auth.require(branch,"member");
    return Optional.ofNullable(members.own(branch,actor.id())).orElseThrow(()->new NotFoundException("Expediente propio no encontrado"));
  }
  @Transactional
  public Map<String,Object> add(UUID branch,MemberRequest data) {
    auth.require(branch,"owner");
    if (data==null || data.userId()==null) throw new DomainException("userId de un usuario existente es obligatorio");
    var user=accounts.findCredentialsById(data.userId())
      .orElseThrow(()->new DomainException("Usuario global no encontrado"));
    if (!"active".equals(user.status())) throw new DomainException("Solo se vinculan usuarios globales activos");
    BranchMember m=new BranchMember();m.branchId=branch;m.userId=data.userId();
    m.memberCode=data.code()==null || data.code().isBlank()
      ? "M-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT) : data.code();
    apply(m,data);
    create(m,auth.actor().id());
    accounts.setDefaultBranchIfMissing(data.userId(),branch);
    return members.detail(branch,m.id);
  }
  @Transactional
  public Map<String,Object> change(UUID branch,UUID id,MemberRequest data) {
    auth.require(branch,"owner");
    if (data==null) throw new DomainException("Datos obligatorios");
    if (data.userId()!=null) throw new DomainException("No se permite cambiar userId en un expediente");
    BranchMember m=getOr404(id,branch);
    if (data.code()!=null) {
      if (data.code().isBlank()) throw new DomainException("Código de miembro vacío");
      m.memberCode=data.code();
    }
    apply(m,data);update(m,auth.actor().id());return members.detail(branch,id);
  }
  private void apply(BranchMember m,MemberRequest d) {
    if(d.birthDate()!=null)m.birthDate=d.birthDate();if(d.address()!=null)m.address=d.address();
    if(d.emergencyContactName()!=null)m.emergencyContactName=d.emergencyContactName();
    if(d.emergencyContactPhone()!=null)m.emergencyContactPhone=d.emergencyContactPhone();
    if(d.notes()!=null)m.notes=d.notes();
    if(d.startDate()!=null)m.joinedOn=d.startDate();if(d.active()!=null)m.isActive=d.active();
  }
  @Transactional(readOnly = true)
  public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> photo(UUID branch, UUID memberId) {
    requirePhotoAccess(branch, memberId);
    MemberPhoto p = photos.activeForMember(branch, memberId)
      .orElseThrow(() -> new NotFoundException("Fotografía no encontrada"));
    return org.springframework.http.ResponseEntity.ok()
      .contentType(org.springframework.http.MediaType.parseMediaType(p.getMimeType()))
      .header("X-Content-Type-Options", "nosniff")
      .cacheControl(org.springframework.http.CacheControl.noStore())
      .body(files.read(p.getStorageKey()));
  }

  @Transactional
  public Map<String, Object> uploadPhoto(UUID branch, UUID memberId, MultipartFile file) {
    Actor actor = requirePhotoAccess(branch, memberId);
    getOr404(memberId, branch);
    if (file == null || file.isEmpty()) throw new DomainException("Fotografía obligatoria");
    LocalImageStorage.Saved saved = files.save("member-photos/" + branch, file);
    if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
      org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
        new org.springframework.transaction.support.TransactionSynchronization() {
          @Override public void afterCompletion(int status) {
            if (status != STATUS_COMMITTED) files.discard(saved.key());
          }
        });
    }
    photos.activeForMember(branch, memberId).ifPresent(old -> {
      audit.setDeleteFields(old, actor.id());
      photos.update(old);
    });
    MemberPhoto p = new MemberPhoto();
    p.setBranchId(branch);
    p.setMemberId(memberId);
    p.setStorageKey(saved.key());
    p.setMimeType(saved.mime());
    p.setByteSize(saved.size());
    p.setUploadedBy(actor.id());
    audit.setAuditFields(p, actor.id(), true);
    photos.create(p);
    return Map.of("photoUrl", "/api/v1/branches/" + branch + "/members/" + memberId + "/photo");
  }

  @Transactional
  public void removePhoto(UUID branch, UUID memberId) {
    Actor actor = requirePhotoAccess(branch, memberId);
    MemberPhoto p = photos.activeForMember(branch, memberId)
      .orElseThrow(() -> new NotFoundException("Fotografía no encontrada"));
    audit.setDeleteFields(p, actor.id());
    photos.update(p);
  }

  private Actor requirePhotoAccess(UUID branch, UUID memberId) {
    Actor actor = auth.require(branch, "owner", "trainer", "member");
    boolean staff = auth.hasRole(branch, "owner") || auth.hasRole(branch, "trainer");
    if (!staff) {
      Map<String, Object> own = members.own(branch, actor.id());
      if (own == null || !String.valueOf(memberId).equals(String.valueOf(own.get("id"))))
        throw new NotFoundException("Expediente propio no encontrado");
    }
    return actor;
  }

  @Transactional
  public void remove(UUID branch,UUID id) {
    auth.require(branch,"owner");BranchMember m=getOr404(id,branch);m.isActive=false;softDelete(m,auth.actor().id());
  }
}
