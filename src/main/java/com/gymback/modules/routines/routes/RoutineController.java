package com.gymback.modules.routines.routes;
import com.gymback.modules.routines.dto.*;
import com.gymback.modules.routines.services.RoutineService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/branches/{branchId}/routines")
public class RoutineController {
 private final RoutineService s;public RoutineController(RoutineService s){this.s=s;}
 @GetMapping public List<RoutineView> list(@PathVariable UUID branchId,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="50")int limit){return s.all(branchId,offset,limit);}
 @GetMapping("/{id}")public RoutineView get(@PathVariable UUID branchId,@PathVariable UUID id){return s.one(branchId,id);}
 @PostMapping public ResponseEntity<RoutineView> post(@PathVariable UUID branchId,@RequestBody RoutineRequest d){return ResponseEntity.status(201).body(s.add(branchId,d));}
 @PatchMapping("/{id}")public RoutineView patch(@PathVariable UUID branchId,@PathVariable UUID id,@RequestBody RoutineRequest d){return s.change(branchId,id,d);}
 @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID branchId,@PathVariable UUID id){s.remove(branchId,id);}
}
