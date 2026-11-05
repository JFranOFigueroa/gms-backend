package com.gymback.modules.audit.routes;
import com.gymback.modules.audit.services.AuditEventService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
public class AuditEventController {
  private final AuditEventService service;
  public AuditEventController(AuditEventService service){this.service=service;}
  @GetMapping("/api/v1/gyms/{gymId}/audit-events")
  public List<Map<String,Object>> gym(@PathVariable UUID gymId,@RequestParam(defaultValue="0") int offset,
      @RequestParam(defaultValue="50") int limit){return service.byGym(gymId,offset,limit);}
  @GetMapping("/api/v1/audit-events")
  public List<Map<String,Object>> global(@RequestParam(required=false) UUID gymId,
      @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit){
    return service.global(gymId,offset,limit);
  }
}
