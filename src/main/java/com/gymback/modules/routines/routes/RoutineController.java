package com.gymback.modules.routines.routes;
import com.gymback.modules.routines.dto.*;
import com.gymback.modules.routines.services.RoutineService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/gyms/{gymId}/routines")
public class RoutineController {
 private final RoutineService s;public RoutineController(RoutineService s){this.s=s;}
 @GetMapping public List<RoutineView> list(@PathVariable UUID gymId,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="50")int limit){return s.all(gymId,offset,limit);}
 @GetMapping("/{id}")public RoutineView get(@PathVariable UUID gymId,@PathVariable UUID id){return s.one(gymId,id);}
 @PostMapping public ResponseEntity<RoutineView> post(@PathVariable UUID gymId,@RequestBody RoutineRequest d){return ResponseEntity.status(201).body(s.add(gymId,d));}
 @PatchMapping("/{id}")public RoutineView patch(@PathVariable UUID gymId,@PathVariable UUID id,@RequestBody RoutineRequest d){return s.change(gymId,id,d);}
 @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID gymId,@PathVariable UUID id){s.remove(gymId,id);}
}
