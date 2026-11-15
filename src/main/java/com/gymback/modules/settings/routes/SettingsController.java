package com.gymback.modules.settings.routes;
import com.gymback.modules.settings.dto.*;
import com.gymback.modules.settings.services.SettingsService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/v1/branches/{branchId}")
public class SettingsController {
 private final SettingsService s;public SettingsController(SettingsService s){this.s=s;}
 @GetMapping("/settings")public Map<String,Object> get(@PathVariable UUID branchId){return s.read(branchId);}
 @PatchMapping("/settings")public Map<String,Object> patch(@PathVariable UUID branchId,@RequestBody SettingsPatch d){return s.patch(branchId,d);}
 @GetMapping("/modules")public List<Map<String,Object>> modules(@PathVariable UUID branchId){return s.modules(branchId);}
 @PatchMapping("/modules/{key}")public List<Map<String,Object>> toggle(@PathVariable UUID branchId,@PathVariable String key,@RequestBody ModulePatch d){return s.toggle(branchId,key,d);}
}
