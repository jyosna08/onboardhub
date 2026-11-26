package com.onboardhub.asset.dto;

import com.onboardhub.asset.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateAssetRequest(
        @NotNull AssetType type,
        @NotBlank @Size(max = 60) String make,
        @NotBlank @Size(max = 80) String model,
        @NotBlank @Size(max = 80) String serialNumber,
        @PastOrPresent LocalDate purchaseDate) {
}
