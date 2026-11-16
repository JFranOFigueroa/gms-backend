package com.gymback.modules.settings.dto;
public record SettingsPatch(Boolean allowMultipleNormalClassesPerDay,Integer guestBookingLimit,Integer checkinQrTtlSeconds){}
