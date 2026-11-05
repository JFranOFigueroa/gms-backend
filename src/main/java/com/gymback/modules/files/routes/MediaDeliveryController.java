package com.gymback.modules.files.routes;
import com.gymback.modules.files.services.MediaDeliveryService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
public class MediaDeliveryController {
 private final MediaDeliveryService service;
 public MediaDeliveryController(MediaDeliveryService service){this.service=service;}
 @GetMapping("/api/v1/gyms/{gymId}/media-assets/{id}/content")
 public ResponseEntity<Resource> privateImage(@PathVariable UUID gymId,@PathVariable UUID id){return service.privateImage(gymId,id);}
 @GetMapping("/api/v1/public/media/{id}")
 public ResponseEntity<Resource> publicImage(@PathVariable UUID id){return service.publicImage(id);}
}
