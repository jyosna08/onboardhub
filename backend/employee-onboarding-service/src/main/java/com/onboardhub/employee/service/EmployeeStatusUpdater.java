package com.onboardhub.employee.service;

import com.onboardhub.employee.entity.Employee;
import com.onboardhub.employee.enums.EmployeeStatus;
import com.onboardhub.employee.enums.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EmployeeStatusUpdater {
    private final EmployeeStatusCalculator calculator;

    public EmployeeStatusUpdater(EmployeeStatusCalculator calculator) { this.calculator = calculator; }

    public void update(Employee employee, List<TaskStatus> statuses, LocalDateTime now) {
        EmployeeStatus status = calculator.calculate(statuses);
        if (status == EmployeeStatus.ACTIVE) employee.completeOnboarding(now);
        else if (status == EmployeeStatus.ONBOARDING) employee.markOnboardingStarted();
        else employee.markPending();
        employee.setUpdatedAt(now);
    }
}
