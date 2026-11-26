package com.onboardhub.employee.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.onboardhub.employee.enums.AssetType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetRemoteResponse(Long id, String assetTag, AssetType type, String make, String model,
        String serialNumber, String status) {
}
