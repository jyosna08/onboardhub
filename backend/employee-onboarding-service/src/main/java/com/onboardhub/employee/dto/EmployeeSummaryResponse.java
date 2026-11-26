package com.onboardhub.employee.dto;

import com.onboardhub.employee.enums.Department;
import com.onboardhub.employee.enums.EmployeeStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EmployeeSummaryResponse(Long id, String employeeCode, String fullName, String email,
        Department department, String jobTitle, LocalDate joiningDate, EmployeeStatus status,
        LocalDateTime onboardingCompletedAt, LocalDateTime createdAt, long tasksDone, long tasksTotal) {
}
