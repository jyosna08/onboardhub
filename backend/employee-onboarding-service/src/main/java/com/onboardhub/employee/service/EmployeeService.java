package com.onboardhub.employee.service;

import com.onboardhub.employee.dto.CreateEmployeeRequest;
import com.onboardhub.employee.dto.DashboardResponse;
import com.onboardhub.employee.dto.EmployeeDetailResponse;
import com.onboardhub.employee.dto.EmployeeSummaryResponse;
import com.onboardhub.employee.dto.TaskResponse;
import com.onboardhub.employee.dto.UpdateEmployeeRequest;
import com.onboardhub.employee.dto.UpdateTaskStatusRequest;
import com.onboardhub.employee.enums.Department;
import com.onboardhub.employee.enums.EmployeeStatus;
import com.onboardhub.employee.security.AuthenticatedUser;
import java.util.List;

public interface EmployeeService {
    EmployeeDetailResponse createEmployee(CreateEmployeeRequest request, AuthenticatedUser caller);
    List<EmployeeSummaryResponse> listEmployees(EmployeeStatus status, Department department, String search);
    EmployeeDetailResponse getEmployee(Long id, AuthenticatedUser caller);
    EmployeeDetailResponse getMyEmployee(AuthenticatedUser caller);
    EmployeeDetailResponse updateEmployee(Long id, UpdateEmployeeRequest request);
    TaskResponse updateTaskStatus(Long taskId, UpdateTaskStatusRequest request, AuthenticatedUser caller);
    DashboardResponse dashboard();
}
