package com.gymback.modules.site.dto;

import java.util.Map;
import java.util.UUID;

public final class SiteDtos {
  private SiteDtos() {}

  public record SiteSettingsPatch(
      String siteName, String shortName, UUID logoAssetId, String contactEmail,
      String contactPhone, String address, String timezone, String currency,
      String themePreset, Map<String, Object> themeOverrides) {}

  public record SiteLandingBlockRequest(
      String blockType, Integer sortOrder, Boolean enabled, Map<String, Object> blockData) {}

  public record SiteMediaAssetRequest(
      String name, String mediaType, String externalUrl, String mimeType, Long byteSize) {}
}
