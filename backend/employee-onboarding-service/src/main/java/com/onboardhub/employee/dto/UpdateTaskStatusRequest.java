package com.onboardhub.employee.dto;

import com.onboardhub.employee.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {
}
