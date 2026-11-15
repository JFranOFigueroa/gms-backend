package com.gymback.modules.settings.interfaces;
import java.util.*;
public interface ISettingsRepository {
 Map<String,Object> settings(UUID branch);
 Map<String,Object> patch(UUID branch,Map<String,Object> allowedFields);
 List<Map<String,Object>> modules(UUID branch);
 void toggle(UUID branch,String key,boolean enabled,UUID actor);
}
