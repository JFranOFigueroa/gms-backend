package com.gymback.modules.checkin.routes;
import com.gymback.modules.checkin.dto.CheckinQrDtos.PublicConfig;
import com.gymback.modules.checkin.services.KioskDeviceService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestController
@RequestMapping("/api/v1/public/branches")
public class PublicCheckinController {
 private final KioskDeviceService service;
 public PublicCheckinController(KioskDeviceService service){this.service=service;}
 @GetMapping("/{code}/checkin-config") public ResponseEntity<PublicConfig> config(@PathVariable String code){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.publicConfig(code));
 }
}
