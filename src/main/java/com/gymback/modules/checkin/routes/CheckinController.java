package com.gymback.modules.checkin.routes;
import com.gymback.modules.checkin.dto.CheckinRequest;
import com.gymback.modules.checkin.dto.CheckinQrDtos.*;
import jakarta.validation.Valid;
import com.gymback.modules.checkin.services.CheckinService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/v1/branches/{branchId}/checkins")
public class CheckinController {
 private final CheckinService s;public CheckinController(CheckinService s){this.s=s;}
 @GetMapping public List<Map<String,Object>> audit(@PathVariable UUID branchId,@RequestParam(defaultValue="50")int limit){return s.audit(branchId,limit);}
 @GetMapping("/me")public Map<String,Object> me(@PathVariable UUID branchId,@RequestParam(required=false)LocalDate from,
   @RequestParam(required=false)LocalDate to,@RequestParam(defaultValue="50")int limit){return s.own(branchId,from,to,limit);}
 @PostMapping public ResponseEntity<Map<String,Object>> manual(@PathVariable UUID branchId,@RequestBody CheckinRequest d){return ResponseEntity.status(201).body(s.manual(branchId,d));}
 @PostMapping("/scan-branch-qr") public ResponseEntity<CheckinResult> scanBranch(@PathVariable UUID branchId,
  @Valid @RequestBody ScanBranch request,@RequestHeader("Idempotency-Key")String key){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(s.scanBranch(branchId,request.kioskToken(),key));
 }
 @PostMapping("/me/qr") public ResponseEntity<IssuedQr> memberQr(@PathVariable UUID branchId){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(s.myMemberQr(branchId));
 }
 @PostMapping("/me/code") public ResponseEntity<IssuedCode> myCode(@PathVariable UUID branchId){
  return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(s.issueManualCode(branchId));
 }
 @GetMapping("/stats") public Stats stats(@PathVariable UUID branchId,
     @RequestParam(required=false)LocalDate from,@RequestParam(required=false)LocalDate to){return s.stats(branchId,from,to);}
}
