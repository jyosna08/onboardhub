package com.onboardhub.employee.dto;

import com.onboardhub.employee.enums.Department;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateEmployeeRequest(
        @Size(min = 1, max = 100) String fullName,
        Department department,
        @Size(min = 1, max = 100) String jobTitle,
        LocalDate joiningDate) {
}
