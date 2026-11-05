package com.gymback.modules.settings.routes;
import com.gymback.modules.settings.dto.*;
import com.gymback.modules.settings.services.SettingsService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/gyms/{gymId}")
public class SettingsController {
 private final SettingsService s;public SettingsController(SettingsService s){this.s=s;}
 @GetMapping("/settings")public Map<String,Object> get(@PathVariable UUID gymId){return s.read(gymId);}
 @PatchMapping("/settings")public Map<String,Object> patch(@PathVariable UUID gymId,@RequestBody SettingsPatch d){return s.patch(gymId,d);}
 @GetMapping("/modules")public List<Map<String,Object>> modules(@PathVariable UUID gymId){return s.modules(gymId);}
 @PatchMapping("/modules/{key}")public List<Map<String,Object>> toggle(@PathVariable UUID gymId,@PathVariable String key,@RequestBody ModulePatch d){return s.toggle(gymId,key,d);}
}
