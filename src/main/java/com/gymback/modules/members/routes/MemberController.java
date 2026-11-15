package com.gymback.modules.members.routes;
import com.gymback.modules.members.dto.MemberRequest;
import com.gymback.modules.members.services.MemberService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/branches/{branchId}/members")
public class MemberController {
  private final MemberService s;public MemberController(MemberService s){this.s=s;}
  @GetMapping public List<Map<String,Object>> list(@PathVariable UUID branchId,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="50")int limit){return s.all(branchId,offset,limit);}
  @GetMapping("/me")public Map<String,Object> me(@PathVariable UUID branchId){return s.own(branchId);}
  @GetMapping("/{id}") public Map<String,Object> get(@PathVariable UUID branchId,@PathVariable UUID id){return s.one(branchId,id);}
  @PostMapping public ResponseEntity<Map<String,Object>> create(@PathVariable UUID branchId,@RequestBody MemberRequest data){return ResponseEntity.status(201).body(s.add(branchId,data));}
  @PatchMapping("/{id}")public Map<String,Object> patch(@PathVariable UUID branchId,@PathVariable UUID id,@RequestBody MemberRequest data){return s.change(branchId,id,data);}
  @GetMapping("/{id}/photo")
  public ResponseEntity<org.springframework.core.io.Resource> photo(
      @PathVariable UUID branchId, @PathVariable UUID id) {
    return s.photo(branchId, id);
  }
  @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> uploadPhoto(
      @PathVariable UUID branchId, @PathVariable UUID id,
      @RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
    return s.uploadPhoto(branchId, id, file);
  }
  @DeleteMapping("/{id}/photo") @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePhoto(@PathVariable UUID branchId, @PathVariable UUID id) {
    s.removePhoto(branchId, id);
  }
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID branchId,@PathVariable UUID id){s.remove(branchId,id);}
}
