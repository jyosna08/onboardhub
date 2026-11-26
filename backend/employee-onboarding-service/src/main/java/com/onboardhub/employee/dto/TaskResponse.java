package com.onboardhub.employee.dto;

import com.onboardhub.employee.enums.AssetType;
import com.onboardhub.employee.enums.OwnerRole;
import com.onboardhub.employee.enums.TaskCategory;
import com.onboardhub.employee.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(Long id, String title, TaskCategory category, OwnerRole ownerRole, TaskStatus status,
        LocalDate dueDate, boolean overdue, AssetType assetType, Long assetId, String assetTag,
        String assetLabel, LocalDateTime completedAt, String completedByName) {
}
