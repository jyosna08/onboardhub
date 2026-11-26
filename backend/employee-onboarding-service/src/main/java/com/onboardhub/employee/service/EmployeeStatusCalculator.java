package com.onboardhub.employee.service;

import com.onboardhub.employee.enums.EmployeeStatus;
import com.onboardhub.employee.enums.TaskStatus;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EmployeeStatusCalculator {
    public EmployeeStatus calculate(List<TaskStatus> statuses) {
        if (statuses.isEmpty()) return EmployeeStatus.PENDING;
        if (statuses.stream().allMatch(status -> status == TaskStatus.DONE)) return EmployeeStatus.ACTIVE;
        if (statuses.stream().anyMatch(status -> status != TaskStatus.TODO)) return EmployeeStatus.ONBOARDING;
        return EmployeeStatus.PENDING;
    }
}
