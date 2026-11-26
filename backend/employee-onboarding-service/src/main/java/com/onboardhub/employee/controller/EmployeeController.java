package com.onboardhub.employee.controller;

import com.onboardhub.employee.dto.CreateEmployeeRequest;
import com.onboardhub.employee.dto.EmployeeDetailResponse;
import com.onboardhub.employee.dto.EmployeeSummaryResponse;
import com.onboardhub.employee.dto.UpdateEmployeeRequest;
import com.onboardhub.employee.enums.Department;
import com.onboardhub.employee.enums.EmployeeStatus;
import com.onboardhub.employee.security.AuthenticatedUser;
import com.onboardhub.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @Operation(summary = "Create an employee and onboarding checklist")
    public EmployeeDetailResponse create(@Valid @RequestBody CreateEmployeeRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return employeeService.createEmployee(request, caller);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'IT', 'ADMIN')")
    @Operation(summary = "List and filter employees")
    public List<EmployeeSummaryResponse> list(@RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) Department department,
            @RequestParam(required = false) String search) {
        return employeeService.listEmployees(status, department, search);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get the authenticated employee record")
    public EmployeeDetailResponse me(@AuthenticationPrincipal AuthenticatedUser caller) {
        return employeeService.getMyEmployee(caller);
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('HR', 'IT', 'ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get an employee and their checklist")
    public EmployeeDetailResponse get(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return employeeService.getEmployee(id, caller);
    }

    @PatchMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    @Operation(summary = "Update employee details")
    public EmployeeDetailResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return employeeService.updateEmployee(id, request);
    }
}
