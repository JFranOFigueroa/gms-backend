package com.gymback.modules.settings.interfaces;
import java.util.*;
public interface ISettingsRepository {
 Map<String,Object> settings(UUID gym);
 Map<String,Object> patch(UUID gym,Map<String,Object> allowedFields);
 List<Map<String,Object>> modules(UUID gym);
 void toggle(UUID gym,String key,boolean enabled,UUID actor);
}
