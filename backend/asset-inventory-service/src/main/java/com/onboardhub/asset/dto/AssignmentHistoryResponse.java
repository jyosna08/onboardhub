package com.onboardhub.asset.dto;

import java.time.LocalDateTime;

public record AssignmentHistoryResponse(Long id, Long employeeId, String employeeName, String assignedByName,
        LocalDateTime assignedAt, LocalDateTime returnedAt, String returnedByName) {
}
