package com.gymback.modules.checkin.routes;
import com.gymback.modules.checkin.dto.CheckinQrDtos.*;
import com.gymback.modules.checkin.services.KioskDeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/gyms/{gymId}/checkin-devices")
public class CheckinDeviceController {
 private final KioskDeviceService service;
 public CheckinDeviceController(KioskDeviceService service){this.service=service;}
 @GetMapping public List<DeviceView> list(@PathVariable UUID gymId){return service.list(gymId);}
 @PostMapping public ResponseEntity<DeviceCreated> create(@PathVariable UUID gymId,@Valid @RequestBody CreateDevice req){
  return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(service.create(gymId,req));
 }
 @PostMapping("/{deviceId}/reissue-activation") public ResponseEntity<DeviceCreated> reissue(@PathVariable UUID gymId,@PathVariable UUID deviceId){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.reissue(gymId,deviceId));
 }
 @PatchMapping("/{deviceId}") public DeviceView update(@PathVariable UUID gymId,@PathVariable UUID deviceId,@Valid @RequestBody UpdateDevice req){
  return service.change(gymId,deviceId,req);
 }
 @DeleteMapping("/{deviceId}") public ResponseEntity<Void> delete(@PathVariable UUID gymId,@PathVariable UUID deviceId){
  service.delete(gymId,deviceId);return ResponseEntity.noContent().build();
 }
}
