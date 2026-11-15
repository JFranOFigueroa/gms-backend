package com.gymback.modules.calendar.routes;
import com.gymback.modules.calendar.dto.ReservationRequest;
import com.gymback.modules.calendar.services.ReservationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
import java.time.LocalDate;
@RestController @RequestMapping("/api/v1/branches/{branchId}/reservations")
public class ReservationController {
 private final ReservationService s;public ReservationController(ReservationService s){this.s=s;}
 @GetMapping public List<Map<String,Object>> list(@PathVariable UUID branchId,@RequestParam UUID classId){return s.list(branchId,classId);}
 @GetMapping("/me") public Map<String,Object> me(@PathVariable UUID branchId,@RequestParam(required=false)LocalDate from,
   @RequestParam(required=false)LocalDate to,@RequestParam(required=false)String status,
   @RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="50")int limit){return s.own(branchId,from,to,status,offset,limit);}
 @PostMapping public ResponseEntity<Map<String,Object>> post(@PathVariable UUID branchId,@RequestBody ReservationRequest d){return ResponseEntity.status(201).body(s.book(branchId,d));}
 @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID branchId,@PathVariable UUID id){s.cancel(branchId,id);}
}
