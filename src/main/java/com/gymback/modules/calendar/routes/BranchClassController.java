package com.gymback.modules.calendar.routes;
import com.gymback.modules.calendar.services.BranchClassService;
import com.gymback.modules.calendar.dto.BranchClassRequest;
import com.gymback.modules.calendar.model.BranchClass;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
import java.time.LocalDate;
@RestController
@RequestMapping("/api/v1/branches/{branchId}/classes")
public class BranchClassController {
  private final BranchClassService service;
  @GetMapping public List<BranchClass> list(@PathVariable UUID branchId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit,
    @RequestParam(required=false) LocalDate from,@RequestParam(required=false)LocalDate to,@RequestParam(required=false)String status){
      return service.calendar(branchId,offset,limit,from,to,status);
  }
  private final com.gymback.modules.calendar.services.ReservationService reservations;
  public BranchClassController(BranchClassService service,com.gymback.modules.calendar.services.ReservationService reservations){this.service=service;this.reservations=reservations;}
  @GetMapping("/{classId}/availability")public Map<String,Object> availability(@PathVariable UUID branchId,@PathVariable UUID classId){return reservations.availability(branchId,classId);}
  @GetMapping("/{id}")public BranchClass get(@PathVariable UUID branchId,@PathVariable UUID id){return service.one(branchId,id);}
  @PostMapping public ResponseEntity<BranchClass> create(@PathVariable UUID branchId,@RequestBody BranchClassRequest request){return ResponseEntity.status(201).body(service.add(branchId,request));}
  @PatchMapping("/{id}")public BranchClass patch(@PathVariable UUID branchId,@PathVariable UUID id,@RequestBody BranchClassRequest request){return service.change(branchId,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID branchId,@PathVariable UUID id){service.remove(branchId,id);}
}
