package com.gymback.modules.site.routes;

import com.gymback.modules.site.dto.SiteDtos.SiteLandingBlockRequest;
import com.gymback.modules.site.model.SiteLandingBlock;
import com.gymback.modules.site.services.SiteLandingBlockService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/site/landing-blocks")
public class SiteLandingBlockController {
  private final SiteLandingBlockService service;
  public SiteLandingBlockController(SiteLandingBlockService service) { this.service = service; }
  @GetMapping public List<SiteLandingBlock> list(
      @RequestParam(defaultValue = "0") int offset,
      @RequestParam(defaultValue = "100") int limit) {
    return service.all(offset, limit);
  }
  @GetMapping("/{id}") public SiteLandingBlock get(@PathVariable UUID id) { return service.one(id); }
  @PostMapping public ResponseEntity<SiteLandingBlock> create(@RequestBody SiteLandingBlockRequest request) {
    return ResponseEntity.status(201).body(service.add(request));
  }
  @PatchMapping("/{id}") public SiteLandingBlock patch(
      @PathVariable UUID id, @RequestBody SiteLandingBlockRequest request) {
    return service.change(id, request);
  }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) { service.remove(id); }
}
