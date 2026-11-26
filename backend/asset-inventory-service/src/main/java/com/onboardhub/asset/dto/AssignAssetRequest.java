package com.onboardhub.asset.dto;

import com.onboardhub.asset.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignAssetRequest(
        @NotNull Long employeeId,
        @NotBlank @Size(max = 100) String employeeName,
        @NotNull AssetType expectedType) {
}
