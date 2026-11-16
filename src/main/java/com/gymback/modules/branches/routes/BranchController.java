package com.gymback.modules.branches.routes;
import com.gymback.modules.branches.services.BranchService;
import com.gymback.modules.branches.dto.BranchRequest;
import com.gymback.modules.branches.model.Branch;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/branches")
public class BranchController {
  private final BranchService service;
  public BranchController(BranchService service){this.service=service;}
  @GetMapping public List<Branch> list(@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {return service.all(null,offset,limit);}
  @GetMapping("/{id}")public Branch get(@PathVariable UUID id){return service.one(null,id);}
  @PostMapping public ResponseEntity<Branch> create(@RequestBody BranchRequest request){return ResponseEntity.status(201).body(service.add(null,request));}
  @PatchMapping("/{id}")public Branch patch(@PathVariable UUID id,@RequestBody BranchRequest request){return service.change(null,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID id){service.remove(null,id);}
}
