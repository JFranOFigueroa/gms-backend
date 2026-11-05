package com.gymback.modules.plans.routes;
import com.gymback.modules.plans.services.MembershipPlanService;
import com.gymback.modules.plans.dto.MembershipPlanRequest;
import com.gymback.modules.plans.model.MembershipPlan;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/plans")
public class MembershipPlanController {
  private final MembershipPlanService service;
  public MembershipPlanController(MembershipPlanService service){this.service=service;}
  @GetMapping public List<MembershipPlan> list(@PathVariable UUID gymId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {return service.all(gymId,offset,limit);}
  @GetMapping("/{id}")public MembershipPlan get(@PathVariable UUID gymId,@PathVariable UUID id){return service.one(gymId,id);}
  @PostMapping public ResponseEntity<MembershipPlan> create(@PathVariable UUID gymId,@RequestBody MembershipPlanRequest request){return ResponseEntity.status(201).body(service.add(gymId,request));}
  @PatchMapping("/{id}")public MembershipPlan patch(@PathVariable UUID gymId,@PathVariable UUID id,@RequestBody MembershipPlanRequest request){return service.change(gymId,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID gymId,@PathVariable UUID id){service.remove(gymId,id);}
}
