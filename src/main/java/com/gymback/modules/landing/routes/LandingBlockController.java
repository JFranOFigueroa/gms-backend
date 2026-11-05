package com.gymback.modules.landing.routes;
import com.gymback.modules.landing.services.LandingBlockService;
import com.gymback.modules.landing.dto.LandingBlockRequest;
import com.gymback.modules.landing.model.LandingBlock;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/landing-blocks")
public class LandingBlockController {
  private final LandingBlockService service;
  public LandingBlockController(LandingBlockService service){this.service=service;}
  @GetMapping public List<LandingBlock> list(@PathVariable UUID gymId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {return service.all(gymId,offset,limit);}
  @GetMapping("/{id}")public LandingBlock get(@PathVariable UUID gymId,@PathVariable UUID id){return service.one(gymId,id);}
  @PostMapping public ResponseEntity<LandingBlock> create(@PathVariable UUID gymId,@RequestBody LandingBlockRequest request){return ResponseEntity.status(201).body(service.add(gymId,request));}
  @PatchMapping("/{id}")public LandingBlock patch(@PathVariable UUID gymId,@PathVariable UUID id,@RequestBody LandingBlockRequest request){return service.change(gymId,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID gymId,@PathVariable UUID id){service.remove(gymId,id);}
}
