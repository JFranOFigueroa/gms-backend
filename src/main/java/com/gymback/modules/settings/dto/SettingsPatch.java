package com.gymback.modules.settings.dto;
import java.util.*;
public record SettingsPatch(String themePreset,Map<String,Object> themeOverrides,Boolean allowMultipleNormalClassesPerDay,Integer guestBookingLimit,Integer checkinQrTtlSeconds,Boolean landingPublished){}
