package com.gymback.modules.checkin.routes;
import com.gymback.modules.checkin.dto.CheckinQrDtos.PublicConfig;
import com.gymback.modules.checkin.services.KioskDeviceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestController
@RequestMapping("/api/v1/public/gyms")
public class PublicCheckinController {
 private final KioskDeviceService service;
 public PublicCheckinController(KioskDeviceService service){this.service=service;}
 @GetMapping("/{slug}/checkin-config") public ResponseEntity<PublicConfig> config(@PathVariable String slug){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.publicConfig(slug));
 }
}
