package com.gymback.modules.gyms.services;
import com.gymback.modules.gyms.model.Gym;
import com.gymback.modules.gyms.interfaces.IGymRepository;
import com.gymback.modules.gyms.dto.GymRequest;
import com.gymback.shared.services.*;
import com.gymback.shared.exceptions.DomainException;
import com.gymback.shared.exceptions.ConflictException;
import com.gymback.security.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class GymService extends BaseService<Gym> {
  private final AuthorizationService auth;
  public GymService(IGymRepository repo,AuditService audit,AuthorizationService auth) {super(repo,audit);this.auth=auth;}
  @Transactional(readOnly=true) public List<Gym> all(UUID gymId,int offset,int limit){ auth.platformAdmin(); return list(gymId,offset,limit); }
  @Transactional(readOnly=true) public Gym one(UUID gymId,UUID id){ auth.platformAdmin(); return getOr404(id,gymId); }
  @Transactional public Gym add(UUID gymId,GymRequest req){
    Actor actor=auth.platformAdmin();
    if(req==null || req.name()==null || req.name().isBlank())throw new DomainException("name es obligatorio");
    IGymRepository gyms=(IGymRepository)repo;
    String slug=SlugPolicy.normalize(req.slug()==null || req.slug().isBlank()?req.name():req.slug());
    if(req.slug()==null || req.slug().isBlank()){
      String base=slug;int suffix=2;
      while(gyms.slugExists(slug,null)){
        String tail="-"+suffix++;
        slug=base.substring(0,Math.min(base.length(),80-tail.length()))+tail;
        if(suffix>1000)throw new ConflictException("slug_taken","No existe enlace disponible");
      }
    } else if(gyms.slugExists(slug,null))throw new ConflictException("slug_taken","El enlace ya está ocupado");
    Gym e=new Gym();

    if(req.name()!=null) e.setName(req.name()); if(req.shortName()!=null) e.setShortName(req.shortName()); e.setSlug(slug); if(req.address()!=null) e.setAddress(req.address()); if(req.phone()!=null) e.setPhone(req.phone()); if(req.logoUrl()!=null) e.setLogoUrl(req.logoUrl()); if(req.timezone()!=null) e.setTimezone(req.timezone()); if(req.currency()!=null) e.setCurrency(req.currency()); if(req.status()!=null)e.setStatus(req.status());
    e.setCreatedBy(actor.id());
    validate(e);
    return create(e,actor.id());
  }
  @Transactional public Gym change(UUID gymId,UUID id,GymRequest req){
    Actor actor=auth.platformAdmin();
    Gym e=getOr404(id,gymId);
    if(req.name()!=null) e.setName(req.name()); if(req.shortName()!=null) e.setShortName(req.shortName()); if(req.slug()!=null){String newSlug=SlugPolicy.normalize(req.slug());
      if(((IGymRepository)repo).slugExists(newSlug,id))throw new ConflictException("slug_taken","El enlace ya está ocupado");
      e.setSlug(newSlug);
    } if(req.address()!=null) e.setAddress(req.address()); if(req.phone()!=null) e.setPhone(req.phone()); if(req.logoUrl()!=null) e.setLogoUrl(req.logoUrl()); if(req.timezone()!=null) e.setTimezone(req.timezone()); if(req.currency()!=null) e.setCurrency(req.currency()); if(req.status()!=null)e.setStatus(req.status());
    validate(e);
    return update(e,actor.id());
  }
  @Transactional public void remove(UUID gymId, UUID id) {
      Actor actor=auth.platformAdmin();
      Gym e=getOr404(id,gymId);
      e.setStatus("archived");
      softDelete(e,actor.id());
  }
  private void validate(Gym e){ if(!Set.of("active","suspended","archived").contains(e.getStatus()))throw new DomainException("Estado inválido"); if(e.getSlug()==null || !e.getSlug().matches("[a-z0-9-]{3,80}"))throw new DomainException("Enlace inválido"); }
}
