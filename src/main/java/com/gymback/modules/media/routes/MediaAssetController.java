package com.gymback.modules.media.routes;
import com.gymback.modules.media.services.MediaAssetService;
import com.gymback.modules.media.dto.MediaAssetRequest;
import com.gymback.modules.media.model.MediaAsset;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/media-assets")
public class MediaAssetController {
  private final MediaAssetService service;
  public MediaAssetController(MediaAssetService service){this.service=service;}
  @GetMapping public List<MediaAsset> list(@PathVariable UUID gymId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="50") int limit) {return service.all(gymId,offset,limit);}
  @GetMapping("/{id}")public MediaAsset get(@PathVariable UUID gymId,@PathVariable UUID id){return service.one(gymId,id);}
  @PostMapping public ResponseEntity<MediaAsset> create(@PathVariable UUID gymId,@RequestBody MediaAssetRequest request){return ResponseEntity.status(201).body(service.add(gymId,request));}
  @PostMapping(value="/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<MediaAsset> upload(@PathVariable UUID gymId,
    @RequestPart("file") org.springframework.web.multipart.MultipartFile file,
    @RequestParam(required=false) String name){return ResponseEntity.status(201).body(service.upload(gymId,file,name));}
  @PatchMapping("/{id}")public MediaAsset patch(@PathVariable UUID gymId,@PathVariable UUID id,@RequestBody MediaAssetRequest request){return service.change(gymId,id,request);}
  @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@PathVariable UUID gymId,@PathVariable UUID id){service.remove(gymId,id);}
}
