package com.gymback.modules.members.routes;
import com.gymback.modules.members.dto.MemberRequest;
import com.gymback.modules.members.services.MemberService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/gyms/{gymId}/members")
public class MemberController {
  private final MemberService s;public MemberController(MemberService s){this.s=s;}
  @GetMapping public List<Map<String,Object>> list(@PathVariable UUID gymId,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="50")int limit){return s.all(gymId,offset,limit);}
  @GetMapping("/me")public Map<String,Object> me(@PathVariable UUID gymId){return s.own(gymId);}
  @GetMapping("/{id}") public Map<String,Object> get(@PathVariable UUID gymId,@PathVariable UUID id){return s.one(gymId,id);}
  @PostMapping public ResponseEntity<Map<String,Object>> create(@PathVariable UUID gymId,@RequestBody MemberRequest data){return ResponseEntity.status(201).body(s.add(gymId,data));}
  @PatchMapping("/{id}")public Map<String,Object> patch(@PathVariable UUID gymId,@PathVariable UUID id,@RequestBody MemberRequest data){return s.change(gymId,id,data);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID gymId,@PathVariable UUID id){s.remove(gymId,id);}
}
