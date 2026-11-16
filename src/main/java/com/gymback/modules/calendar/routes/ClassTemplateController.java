package com.gymback.modules.calendar.routes;
import com.gymback.modules.calendar.services.ClassTemplateService;
import com.gymback.modules.calendar.dto.ClassTemplateRequest;
import com.gymback.modules.calendar.model.ClassTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/branches/{branchId}/class-templates")
public class ClassTemplateController {
  private final ClassTemplateService service;
  public ClassTemplateController(ClassTemplateService service){this.service=service;}
  @GetMapping public List<ClassTemplate> list(@PathVariable UUID branchId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {return service.all(branchId,offset,limit);}
  @GetMapping("/{id}")public ClassTemplate get(@PathVariable UUID branchId,@PathVariable UUID id){return service.one(branchId,id);}
  @PostMapping public ResponseEntity<ClassTemplate> create(@PathVariable UUID branchId,@RequestBody ClassTemplateRequest request){return ResponseEntity.status(201).body(service.add(branchId,request));}
  @PatchMapping("/{id}")public ClassTemplate patch(@PathVariable UUID branchId,@PathVariable UUID id,@RequestBody ClassTemplateRequest request){return service.change(branchId,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID branchId,@PathVariable UUID id){service.remove(branchId,id);}
}
