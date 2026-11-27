package com.onboardhub.asset.mapper;

import com.onboardhub.asset.dto.AssetResponse;
import com.onboardhub.asset.dto.AssignmentHistoryResponse;
import com.onboardhub.asset.entity.Asset;
import com.onboardhub.asset.entity.AssetAssignment;
import org.springframework.stereotype.Component;

@Component
public class AssetMapper {
    public AssetResponse toResponse(Asset asset) {
        return new AssetResponse(asset.getId(), asset.getAssetTag(), asset.getType(), asset.getMake(),
                asset.getModel(), asset.getSerialNumber(), asset.getStatus(), asset.getPurchaseDate(),
                asset.getAssignedEmployeeId(), asset.getAssignedEmployeeName(), asset.getAssignedAt(),
                asset.getCreatedAt());
    }

    public AssignmentHistoryResponse toHistory(AssetAssignment assignment) {
        return new AssignmentHistoryResponse(assignment.getId(), assignment.getEmployeeId(),
                assignment.getEmployeeName(), assignment.getAssignedByName(), assignment.getAssignedAt(),
                assignment.getReturnedAt(), assignment.getReturnedByName());
    }
}
