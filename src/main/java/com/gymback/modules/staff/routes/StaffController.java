package com.gymback.modules.staff.routes;
import com.gymback.modules.staff.dto.StaffRequest;
import com.gymback.modules.staff.services.StaffService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/gyms/{gymId}/staff")
public class StaffController {
 private final StaffService s;public StaffController(StaffService s){this.s=s;}
 @GetMapping public List<Map<String,Object>> list(@PathVariable UUID gymId){return s.all(gymId);}
 @GetMapping("/{id}")public Map<String,Object> get(@PathVariable UUID gymId,@PathVariable UUID id){return s.one(gymId,id);}
 @PostMapping public ResponseEntity<Map<String,Object>> create(@PathVariable UUID gymId,@RequestBody StaffRequest d){return ResponseEntity.status(201).body(s.add(gymId,d));}
 @PatchMapping("/{id}")public Map<String,Object> patch(@PathVariable UUID gymId,@PathVariable UUID id,@RequestBody StaffRequest d){return s.change(gymId,id,d);}
 @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID gymId,@PathVariable UUID id){s.remove(gymId,id);}
}
