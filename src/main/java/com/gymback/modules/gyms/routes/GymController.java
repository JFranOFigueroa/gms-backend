package com.gymback.modules.gyms.routes;
import com.gymback.modules.gyms.services.GymService;
import com.gymback.modules.gyms.dto.GymRequest;
import com.gymback.modules.gyms.model.Gym;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/gyms")
public class GymController {
  private final GymService service;
  public GymController(GymService service){this.service=service;}
  @GetMapping public List<Gym> list(@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {return service.all(null,offset,limit);}
  @GetMapping("/{id}")public Gym get(@PathVariable UUID id){return service.one(null,id);}
  @PostMapping public ResponseEntity<Gym> create(@RequestBody GymRequest request){return ResponseEntity.status(201).body(service.add(null,request));}
  @PatchMapping("/{id}")public Gym patch(@PathVariable UUID id,@RequestBody GymRequest request){return service.change(null,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID id){service.remove(null,id);}
}
