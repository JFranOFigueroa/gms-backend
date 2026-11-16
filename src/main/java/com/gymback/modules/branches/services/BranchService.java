package com.gymback.modules.branches.services;
import com.gymback.modules.branches.model.Branch;
import com.gymback.modules.branches.interfaces.IBranchRepository;
import com.gymback.modules.branches.dto.BranchRequest;
import com.gymback.shared.services.*;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.exceptions.ConflictException;
import com.gymback.security.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class BranchService extends BaseService<Branch> {
  private final AuthorizationService auth;
  public BranchService(IBranchRepository repo,AuditService audit,AuthorizationService auth) {super(repo,audit);this.auth=auth;}
  @Transactional(readOnly=true) public List<Branch> all(UUID branchId,int offset,int limit){ auth.platformAdmin(); return list(branchId,offset,limit); }
  @Transactional(readOnly=true) public Branch one(UUID branchId,UUID id){ auth.platformAdmin(); return getOr404(id,branchId); }
  @Transactional public Branch add(UUID branchId,BranchRequest req){
    Actor actor=auth.platformAdmin();
    if(req==null || req.name()==null || req.name().isBlank())throw new DomainException("name es obligatorio");
    IBranchRepository branches=(IBranchRepository)repo;
    String code=BranchCodePolicy.normalize(req.code()==null || req.code().isBlank()?req.name():req.code());
    if(req.code()==null || req.code().isBlank()){
      String base=code;int suffix=2;
      while(branches.codeExists(code,null)){
        String tail="-"+suffix++;
        code=base.substring(0,Math.min(base.length(),80-tail.length()))+tail;
        if(suffix>1000)throw new ConflictException("code_taken","No existe código disponible");
      }
    } else if(branches.codeExists(code,null))throw new ConflictException("code_taken","El código ya está ocupado");
    Branch e=new Branch();

    if(req.name()!=null) e.setName(req.name()); if(req.shortName()!=null) e.setShortName(req.shortName()); e.setCode(code); if(req.address()!=null) e.setAddress(req.address()); if(req.phone()!=null) e.setPhone(req.phone()); if(req.logoUrl()!=null) e.setLogoUrl(req.logoUrl()); if(req.timezone()!=null) e.setTimezone(req.timezone()); if(req.currency()!=null) e.setCurrency(req.currency()); if(req.status()!=null)e.setStatus(req.status());
    e.setCreatedBy(actor.id());
    validate(e);
    return create(e,actor.id());
  }
  @Transactional public Branch change(UUID branchId,UUID id,BranchRequest req){
    Actor actor=auth.platformAdmin();
    Branch e=getOr404(id,branchId);
    if(req.name()!=null) e.setName(req.name()); if(req.shortName()!=null) e.setShortName(req.shortName()); if(req.code()!=null){String newCode=BranchCodePolicy.normalize(req.code());
      if(((IBranchRepository)repo).codeExists(newCode,id))throw new ConflictException("code_taken","El código ya está ocupado");
      e.setCode(newCode);
    } if(req.address()!=null) e.setAddress(req.address()); if(req.phone()!=null) e.setPhone(req.phone()); if(req.logoUrl()!=null) e.setLogoUrl(req.logoUrl()); if(req.timezone()!=null) e.setTimezone(req.timezone()); if(req.currency()!=null) e.setCurrency(req.currency()); if(req.status()!=null)e.setStatus(req.status());
    validate(e);
    return update(e,actor.id());
  }
  @Transactional public void remove(UUID branchId, UUID id) {
      Actor actor=auth.platformAdmin();
      Branch e=getOr404(id,branchId);
      e.setStatus("archived");
      softDelete(e,actor.id());
  }
  private void validate(Branch e){
    if(!Set.of("active","suspended","archived").contains(e.getStatus()))throw new DomainException("Estado inválido");
    if(e.getCode()==null || !e.getCode().matches("[a-z0-9-]{3,80}"))throw new DomainException("Código inválido");
    try {java.time.ZoneId.of(e.getTimezone());}
    catch(java.time.DateTimeException | NullPointerException ex){throw new DomainException("Zona horaria inválida");}
  }
}
