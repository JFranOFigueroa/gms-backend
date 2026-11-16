package com.gymback.modules.audit.routes;
import com.gymback.modules.audit.services.AuditEventService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class AuditEventController {
  private final AuditEventService service;
  public AuditEventController(AuditEventService service){this.service=service;}
  @GetMapping("/api/v1/branches/{branchId}/audit-events")
  public List<Map<String,Object>> branch(@PathVariable UUID branchId,@RequestParam(defaultValue="0") int offset,
      @RequestParam(defaultValue="50") int limit){return service.byGym(branchId,offset,limit);}
  @GetMapping("/api/v1/audit-events")
  public List<Map<String,Object>> global(@RequestParam(required=false) UUID branchId,
      @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit){
    return service.global(branchId,offset,limit);
  }
}
