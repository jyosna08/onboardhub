package com.onboardhub.employee.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.onboardhub.employee.enums.AssetType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AssetAssignRequest(Long employeeId, String employeeName, AssetType expectedType) {
}
