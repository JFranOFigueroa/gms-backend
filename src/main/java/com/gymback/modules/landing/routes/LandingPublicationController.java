package com.gymback.modules.landing.routes;
import com.gymback.modules.landing.services.LandingPublicationService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController
@RequestMapping("/api/v1/gyms/{gymId}/landing")
public class LandingPublicationController {
  private final LandingPublicationService service;
  public LandingPublicationController(LandingPublicationService service){this.service=service;}
  /** Alias conservado por compatibilidad con versiones anteriores del frontend. */
  @GetMapping({"/link","/publication"})public Map<String,Object> link(@PathVariable UUID gymId){return service.link(gymId);}
  public record SlugChange(String slug){}
  public record LogoChange(UUID imageAssetId){}
  @PatchMapping("/logo")public Map<String,Object> logo(@PathVariable UUID gymId,@RequestBody LogoChange data){return service.changeLogo(gymId,data.imageAssetId());}
  @PatchMapping("/slug")public Map<String,Object> slug(@PathVariable UUID gymId,@RequestBody SlugChange data){return service.changeSlug(gymId,data.slug());}
}
