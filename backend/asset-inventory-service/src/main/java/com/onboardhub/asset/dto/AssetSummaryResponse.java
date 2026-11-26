package com.onboardhub.asset.dto;

import com.onboardhub.asset.enums.AssetType;
import java.util.List;

public record AssetSummaryResponse(long total, long available, long assigned, long inRepair, long retired,
        List<TypeBreakdown> byType) {
    public record TypeBreakdown(AssetType type, long available, long assigned, long inRepair, long retired) {
    }
}
