package com.gymback.modules.staff.routes;
import com.gymback.modules.staff.dto.StaffRegistrationRequest;
import com.gymback.modules.staff.services.StaffRegistrationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/gyms/{gymId}/staff")
public class StaffRegistrationController {
 private final StaffRegistrationService s;public StaffRegistrationController(StaffRegistrationService s){this.s=s;}
 @PostMapping("/register")public ResponseEntity<Map<String,Object>> register(@PathVariable UUID gymId,@RequestBody StaffRegistrationRequest r){
  return ResponseEntity.status(201).body(s.register(gymId,r));
 }
}
