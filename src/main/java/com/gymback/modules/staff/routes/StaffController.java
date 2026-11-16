package com.gymback.modules.staff.routes;
import com.gymback.modules.staff.dto.StaffRequest;
import com.gymback.modules.staff.services.StaffService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/branches/{branchId}/staff")
public class StaffController {
 private final StaffService s;public StaffController(StaffService s){this.s=s;}
 @GetMapping public List<Map<String,Object>> list(@PathVariable UUID branchId){return s.all(branchId);}
 @GetMapping("/{id}")public Map<String,Object> get(@PathVariable UUID branchId,@PathVariable UUID id){return s.one(branchId,id);}
 @PostMapping public ResponseEntity<Map<String,Object>> create(@PathVariable UUID branchId,@RequestBody StaffRequest d){return ResponseEntity.status(201).body(s.add(branchId,d));}
 @PatchMapping("/{id}")public Map<String,Object> patch(@PathVariable UUID branchId,@PathVariable UUID id,@RequestBody StaffRequest d){return s.change(branchId,id,d);}
 @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID branchId,@PathVariable UUID id){s.remove(branchId,id);}
}
