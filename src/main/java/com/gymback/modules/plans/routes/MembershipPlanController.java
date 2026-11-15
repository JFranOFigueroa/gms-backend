package com.gymback.modules.plans.routes;
import com.gymback.modules.plans.services.MembershipPlanService;
import com.gymback.modules.plans.dto.MembershipPlanRequest;
import com.gymback.modules.plans.model.MembershipPlan;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/branches/{branchId}/plans")
public class MembershipPlanController {
  private final MembershipPlanService service;
  public MembershipPlanController(MembershipPlanService service){this.service=service;}
  @GetMapping public List<MembershipPlan> list(@PathVariable UUID branchId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {return service.all(branchId,offset,limit);}
  @GetMapping("/{id}")public MembershipPlan get(@PathVariable UUID branchId,@PathVariable UUID id){return service.one(branchId,id);}
  @PostMapping public ResponseEntity<MembershipPlan> create(@PathVariable UUID branchId,@RequestBody MembershipPlanRequest request){return ResponseEntity.status(201).body(service.add(branchId,request));}
  @PatchMapping("/{id}")public MembershipPlan patch(@PathVariable UUID branchId,@PathVariable UUID id,@RequestBody MembershipPlanRequest request){return service.change(branchId,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID branchId,@PathVariable UUID id){service.remove(branchId,id);}
}
