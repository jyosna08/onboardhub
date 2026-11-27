package com.onboardhub.employee.service;

import com.onboardhub.employee.enums.AssetType;

public record TaskAssignmentSnapshot(Long taskId, Long employeeId, String employeeName,
        String category, String status, AssetType assetType) {
}
