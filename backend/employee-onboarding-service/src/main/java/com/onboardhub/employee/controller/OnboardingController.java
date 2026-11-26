package com.onboardhub.employee.controller;

import com.onboardhub.employee.dto.DashboardResponse;
import com.onboardhub.employee.dto.AssignAssetRequest;
import com.onboardhub.employee.dto.TaskResponse;
import com.onboardhub.employee.dto.UpdateTaskStatusRequest;
import com.onboardhub.employee.security.AuthenticatedUser;
import com.onboardhub.employee.service.EmployeeService;
import com.onboardhub.employee.service.AssignAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
@Tag(name = "Onboarding")
public class OnboardingController {
    private final EmployeeService employeeService;
    private final AssignAssetService assignAssetService;

    public OnboardingController(EmployeeService employeeService, AssignAssetService assignAssetService) {
        this.employeeService = employeeService;
        this.assignAssetService = assignAssetService;
    }

    @org.springframework.web.bind.annotation.PostMapping("/tasks/{taskId:\\d+}/assign-asset")
    @PreAuthorize("hasAnyRole('IT', 'ADMIN')")
    @Operation(summary = "Assign an asset to an equipment task")
    public TaskResponse assignAsset(@PathVariable Long taskId,
            @Valid @RequestBody AssignAssetRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return assignAssetService.assignAsset(taskId, request.assetId(), caller);
    }

    @PatchMapping("/tasks/{taskId:\\d+}/status")
    @PreAuthorize("hasAnyRole('HR', 'IT', 'ADMIN')")
    @Operation(summary = "Update an onboarding task status")
    public TaskResponse updateTaskStatus(@PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return employeeService.updateTaskStatus(taskId, request, caller);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('HR', 'IT', 'ADMIN')")
    @Operation(summary = "Get onboarding dashboard counts")
    public DashboardResponse dashboard() {
        return employeeService.dashboard();
    }
}
