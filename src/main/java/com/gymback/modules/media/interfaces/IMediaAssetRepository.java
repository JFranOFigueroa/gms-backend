package com.gymback.modules.media.interfaces;
import com.gymback.shared.interfaces.IBaseCrudRepository;
import com.gymback.modules.media.model.MediaAsset;
public interface IMediaAssetRepository extends IBaseCrudRepository<MediaAsset> {
 java.util.Optional<MediaAsset> findPublicCandidate(java.util.UUID id);
}
