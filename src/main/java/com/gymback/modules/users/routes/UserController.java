package com.gymback.modules.users.routes;
import com.gymback.modules.users.dto.*;
import com.gymback.modules.users.services.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/users")
public class UserController {
 private final UserService s;public UserController(UserService s){this.s=s;}
 @PostMapping public ResponseEntity<Map<String,Object>> create(@RequestBody UserCreateRequest r){return ResponseEntity.status(201).body(s.create(r));}
 @GetMapping public Map<String,Object> list(@RequestParam(required=false)String query,@RequestParam(required=false)String status,
    @RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="50")int limit){return s.list(query,status,offset,limit);}
 @GetMapping("/search")public Map<String,Object> search(@RequestParam String email,@RequestParam(required=false) UUID gymId){return s.search(email,gymId);}
 @GetMapping("/{id}")public Map<String,Object> one(@PathVariable UUID id){return s.one(id);}
 @PatchMapping("/{id}")public Map<String,Object> patch(@PathVariable UUID id,@RequestBody UserPatchRequest r){return s.patch(id,r);}
 @PatchMapping("/{id}/status")public Map<String,Object> status(@PathVariable UUID id,@RequestBody StatusPatchRequest r){return s.status(id,r.status());}
}
