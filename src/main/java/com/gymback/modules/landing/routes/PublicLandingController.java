package com.gymback.modules.landing.routes;
import com.gymback.modules.landing.services.LandingPublicationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/public/gyms")
public class PublicLandingController {
 private final LandingPublicationService service;
 public PublicLandingController(LandingPublicationService service){this.service=service;}
 @GetMapping("/{slug}/landing")public ResponseEntity<Map<String,Object>> page(@PathVariable String slug){
   return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.publicLanding(slug));
 }
}
