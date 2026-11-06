package com.gymback.modules.checkin.routes;
import com.gymback.modules.checkin.dto.CheckinRequest;
import com.gymback.modules.checkin.dto.CheckinQrDtos.*;
import jakarta.validation.Valid;
import com.gymback.modules.checkin.services.CheckinService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/v1/gyms/{gymId}/checkins")
public class CheckinController {
 private final CheckinService s;public CheckinController(CheckinService s){this.s=s;}
 @GetMapping public List<Map<String,Object>> audit(@PathVariable UUID gymId,@RequestParam(defaultValue="50")int limit){return s.audit(gymId,limit);}
 @GetMapping("/me")public Map<String,Object> me(@PathVariable UUID gymId,@RequestParam(required=false)LocalDate from,
   @RequestParam(required=false)LocalDate to,@RequestParam(defaultValue="50")int limit){return s.own(gymId,from,to,limit);}
 @PostMapping public ResponseEntity<Map<String,Object>> manual(@PathVariable UUID gymId,@RequestBody CheckinRequest d){return ResponseEntity.status(201).body(s.manual(gymId,d));}
 @PostMapping("/scan-gym-qr") public ResponseEntity<CheckinResult> scanGym(@PathVariable UUID gymId,
  @Valid @RequestBody ScanGym request,@RequestHeader("Idempotency-Key")String key){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(s.scanGym(gymId,request.kioskToken(),key));
 }
 @PostMapping("/me/qr") public ResponseEntity<IssuedQr> memberQr(@PathVariable UUID gymId){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(s.myMemberQr(gymId));
 }
 @PostMapping("/me/code") public ResponseEntity<IssuedCode> myCode(@PathVariable UUID gymId){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(s.issueManualCode(gymId));
 }
 @GetMapping("/stats") public Stats stats(@PathVariable UUID gymId,
     @RequestParam(required=false)LocalDate from,@RequestParam(required=false)LocalDate to){return s.stats(gymId,from,to);}
}
