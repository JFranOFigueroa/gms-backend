package com.gymback.modules.site.routes;

import com.gymback.modules.site.dto.SiteDtos.SiteSettingsPatch;
import com.gymback.modules.site.services.SiteSettingsService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/site/settings")
public class SiteSettingsController {
  private final SiteSettingsService service;
  public SiteSettingsController(SiteSettingsService service) { this.service = service; }
  @GetMapping public Map<String, Object> get() { return service.get(); }
  @PatchMapping public Map<String, Object> patch(@RequestBody SiteSettingsPatch data) { return service.patch(data); }
}
