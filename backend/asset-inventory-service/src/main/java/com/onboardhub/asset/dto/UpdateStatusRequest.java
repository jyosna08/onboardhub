package com.onboardhub.asset.dto;

import com.onboardhub.asset.enums.AssetStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull AssetStatus status) {
}
