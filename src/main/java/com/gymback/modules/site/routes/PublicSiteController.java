package com.gymback.modules.site.routes;

import com.gymback.modules.site.services.PublicSiteService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/public/site")
public class PublicSiteController {
  private final PublicSiteService service;
  public PublicSiteController(PublicSiteService service) { this.service = service; }
  @GetMapping public ResponseEntity<Map<String, Object>> page() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.page());
  }
}
