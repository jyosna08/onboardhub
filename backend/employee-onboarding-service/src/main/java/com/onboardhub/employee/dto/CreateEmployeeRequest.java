package com.onboardhub.employee.dto;

import com.onboardhub.employee.enums.AssetType;
import com.onboardhub.employee.enums.Department;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateEmployeeRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull Department department,
        @NotBlank @Size(max = 100) String jobTitle,
        @NotNull LocalDate joiningDate,
        List<AssetType> requiredAssetTypes) {
}
