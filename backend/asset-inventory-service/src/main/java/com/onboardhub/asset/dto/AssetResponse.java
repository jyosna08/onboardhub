package com.onboardhub.asset.dto;

import com.onboardhub.asset.enums.AssetStatus;
import com.onboardhub.asset.enums.AssetType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssetResponse(Long id, String assetTag, AssetType type, String make, String model,
        String serialNumber, AssetStatus status, LocalDate purchaseDate, Long assignedEmployeeId,
        String assignedEmployeeName, LocalDateTime assignedAt, LocalDateTime createdAt) {
}
