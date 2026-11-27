package com.onboardhub.asset.repository;

import com.onboardhub.asset.enums.AssetStatus;
import com.onboardhub.asset.enums.AssetType;

public interface AssetCountProjection {
    AssetType getType();
    AssetStatus getStatus();
    long getCount();
}
