package com.gymback.modules.files.interfaces;
import java.util.UUID;
public interface IMediaAccessRepository {
  boolean isPublicImage(UUID gymId,UUID assetId);
  boolean isOwnMemberPhoto(UUID gymId,UUID userId,UUID assetId);
}
