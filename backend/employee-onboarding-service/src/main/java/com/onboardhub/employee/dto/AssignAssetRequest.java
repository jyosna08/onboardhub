package com.onboardhub.employee.dto;

import jakarta.validation.constraints.NotNull;

public record AssignAssetRequest(@NotNull Long assetId) {
}
