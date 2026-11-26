package com.onboardhub.employee.dto;

public record DashboardResponse(long pendingEmployees, long onboardingEmployees, long activeEmployees,
        long openTasks, long overdueTasks) {
}
