package com.gymback.modules.audit.services;
import com.gymback.modules.audit.interfaces.IAuditEventRepository;
import com.gymback.security.AuthorizationService;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
public class AuditEventService {
  private final IAuditEventRepository repo;
  private final AuthorizationService auth;
  public AuditEventService(IAuditEventRepository repo,AuthorizationService auth){this.repo=repo;this.auth=auth;}
  private void validate(int offset,int limit){if(offset<0 || limit<1 || limit>100)throw new IllegalArgumentException("Paginación inválida");}
  @Transactional(readOnly=true) public List<Map<String,Object>> byGym(UUID gym,int offset,int limit){
    auth.require(gym,"owner");validate(offset,limit);return repo.list(gym,offset,limit);
  }
  @Transactional(readOnly=true) public List<Map<String,Object>> global(UUID gym,int offset,int limit){
    auth.platformAdmin();validate(offset,limit);return repo.all(gym,offset,limit);
  }
}
