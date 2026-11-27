package com.onboardhub.asset.service;

import com.onboardhub.asset.dto.AssignAssetRequest;
import com.onboardhub.asset.dto.AssetResponse;
import com.onboardhub.asset.dto.AssetSummaryResponse;
import com.onboardhub.asset.dto.AssignmentHistoryResponse;
import com.onboardhub.asset.dto.CreateAssetRequest;
import com.onboardhub.asset.dto.UpdateStatusRequest;
import com.onboardhub.asset.enums.AssetStatus;
import com.onboardhub.asset.enums.AssetType;
import com.onboardhub.asset.security.AuthenticatedUser;
import java.util.List;

public interface AssetService {
    AssetResponse createAsset(CreateAssetRequest request);
    List<AssetResponse> listAssets(AssetType type, AssetStatus status, String search);
    AssetResponse getAsset(Long id);
    List<AssignmentHistoryResponse> assetHistory(Long id);
    AssetSummaryResponse summary();
    AssetResponse updateStatus(Long id, UpdateStatusRequest request);
    AssetResponse assignAsset(Long id, AssignAssetRequest request, AuthenticatedUser caller);
    AssetResponse returnAsset(Long id, AuthenticatedUser caller);
}
