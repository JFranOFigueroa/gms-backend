package com.gymback.modules.site.routes;

import com.gymback.modules.site.dto.SiteDtos.SiteMediaAssetRequest;
import com.gymback.modules.site.model.SiteMediaAsset;
import com.gymback.modules.site.services.SiteMediaService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/site/media")
public class SiteMediaController {
  private final SiteMediaService service;
  public SiteMediaController(SiteMediaService service) { this.service = service; }
  @GetMapping public List<SiteMediaAsset> list() { return service.all(); }
  @GetMapping("/{id}") public SiteMediaAsset get(@PathVariable UUID id) { return service.one(id); }
  @PostMapping public ResponseEntity<SiteMediaAsset> create(@RequestBody SiteMediaAssetRequest request) {
    return ResponseEntity.status(201).body(service.add(request));
  }
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<SiteMediaAsset> upload(
      @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
      @RequestParam(required = false) String name) {
    return ResponseEntity.status(201).body(service.upload(file, name));
  }
  @PatchMapping("/{id}") public SiteMediaAsset patch(
      @PathVariable UUID id, @RequestBody SiteMediaAssetRequest request) {
    return service.change(id, request);
  }
  @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id) { service.remove(id); }
}
