package com.gymback.modules.calendar.routes;
import com.gymback.modules.calendar.services.ClassTemplateService;
import com.gymback.modules.calendar.dto.ClassTemplateRequest;
import com.gymback.modules.calendar.model.ClassTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/class-templates")
public class ClassTemplateController {
  private final ClassTemplateService service;
  public ClassTemplateController(ClassTemplateService service){this.service=service;}
  @GetMapping public List<ClassTemplate> list(@PathVariable UUID gymId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {return service.all(gymId,offset,limit);}
  @GetMapping("/{id}")public ClassTemplate get(@PathVariable UUID gymId,@PathVariable UUID id){return service.one(gymId,id);}
  @PostMapping public ResponseEntity<ClassTemplate> create(@PathVariable UUID gymId,@RequestBody ClassTemplateRequest request){return ResponseEntity.status(201).body(service.add(gymId,request));}
  @PatchMapping("/{id}")public ClassTemplate patch(@PathVariable UUID gymId,@PathVariable UUID id,@RequestBody ClassTemplateRequest request){return service.change(gymId,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID gymId,@PathVariable UUID id){service.remove(gymId,id);}
}
