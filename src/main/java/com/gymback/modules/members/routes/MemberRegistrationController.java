package com.gymback.modules.members.routes;
import com.gymback.modules.members.dto.MemberRegistrationRequest;
import com.gymback.modules.members.services.MemberRegistrationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/gyms/{gymId}/members")
public class MemberRegistrationController {
 private final MemberRegistrationService s;public MemberRegistrationController(MemberRegistrationService s){this.s=s;}
 @PostMapping("/register")public ResponseEntity<Map<String,Object>> register(@PathVariable UUID gymId,@RequestBody MemberRegistrationRequest r){
   return ResponseEntity.status(201).body(s.register(gymId,r));
 }
}
