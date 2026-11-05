package com.gymback.modules.calendar.routes;
import com.gymback.modules.calendar.services.GymClassService;
import com.gymback.modules.calendar.dto.GymClassRequest;
import com.gymback.modules.calendar.model.GymClass;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
import java.time.LocalDate;
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/classes")
public class GymClassController {
  private final GymClassService service;
  @GetMapping public List<GymClass> list(@PathVariable UUID gymId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit,
    @RequestParam(required=false) LocalDate from,@RequestParam(required=false)LocalDate to,@RequestParam(required=false)String status){
      return service.calendar(gymId,offset,limit,from,to,status);
  }
  private final com.gymback.modules.calendar.services.ReservationService reservations;
  public GymClassController(GymClassService service,com.gymback.modules.calendar.services.ReservationService reservations){this.service=service;this.reservations=reservations;}
  @GetMapping("/{classId}/availability")public Map<String,Object> availability(@PathVariable UUID gymId,@PathVariable UUID classId){return reservations.availability(gymId,classId);}
  @GetMapping("/{id}")public GymClass get(@PathVariable UUID gymId,@PathVariable UUID id){return service.one(gymId,id);}
  @PostMapping public ResponseEntity<GymClass> create(@PathVariable UUID gymId,@RequestBody GymClassRequest request){return ResponseEntity.status(201).body(service.add(gymId,request));}
  @PatchMapping("/{id}")public GymClass patch(@PathVariable UUID gymId,@PathVariable UUID id,@RequestBody GymClassRequest request){return service.change(gymId,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID gymId,@PathVariable UUID id){service.remove(gymId,id);}
}
