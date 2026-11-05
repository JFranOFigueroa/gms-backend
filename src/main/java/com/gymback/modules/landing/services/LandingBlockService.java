package com.gymback.modules.landing.services;
import com.gymback.modules.landing.model.LandingBlock;
import com.gymback.modules.landing.interfaces.ILandingBlockRepository;
import com.gymback.modules.landing.dto.LandingBlockRequest;
import com.gymback.shared.services.*;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.security.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class LandingBlockService extends BaseService<LandingBlock> {
  private final AuthorizationService auth;
  public LandingBlockService(ILandingBlockRepository repo,AuditService audit,AuthorizationService auth) {super(repo,audit);this.auth=auth;}
  @Transactional(readOnly=true) public List<LandingBlock> all(UUID gymId,int offset,int limit){ auth.require(gymId,"owner","trainer"); return list(gymId,offset,limit); }
  @Transactional(readOnly=true) public LandingBlock one(UUID gymId,UUID id){ auth.require(gymId,"owner","trainer"); return getOr404(id,gymId); }
  @Transactional public LandingBlock add(UUID gymId,LandingBlockRequest req){
    Actor actor=auth.require(gymId,"owner");
    if(req.blockType()==null || req.blockType().isBlank()) throw new DomainException("blockType es obligatorio");if(req.sortOrder()==null) throw new DomainException("sortOrder es obligatorio");
    LandingBlock e=new LandingBlock();
    e.setGymId(gymId);
    if(req.blockType()!=null) e.setBlockType(req.blockType()); if(req.sortOrder()!=null) e.setSortOrder(req.sortOrder()); if(req.enabled()!=null) e.setEnabled(req.enabled()); if(req.blockData()!=null) e.setBlockData(req.blockData());

    validate(e);
    return create(e,actor.id());
  }
  @Transactional public LandingBlock change(UUID gymId,UUID id,LandingBlockRequest req){
    Actor actor=auth.require(gymId,"owner");
    LandingBlock e=getOr404(id,gymId);
    if(req.blockType()!=null) e.setBlockType(req.blockType()); if(req.sortOrder()!=null) e.setSortOrder(req.sortOrder()); if(req.enabled()!=null) e.setEnabled(req.enabled()); if(req.blockData()!=null) e.setBlockData(req.blockData());
    validate(e);
    return update(e,actor.id());
  }
  @Transactional public void remove(UUID gymId,UUID id){
    Actor actor=auth.require(gymId,"owner");
    LandingBlock e=getOr404(id,gymId);
    softDelete(e,actor.id());
  }
  private void validate(LandingBlock e){ if(e.getSortOrder()==null || e.getSortOrder()<0)throw new DomainException("Orden inválido"); }
}
