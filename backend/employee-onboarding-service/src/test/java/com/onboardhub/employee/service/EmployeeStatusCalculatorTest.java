package com.onboardhub.employee.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.onboardhub.employee.enums.EmployeeStatus;
import com.onboardhub.employee.enums.TaskStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmployeeStatusCalculatorTest {
    private final EmployeeStatusCalculator calculator = new EmployeeStatusCalculator();

    @Test
    void allTodoAndEmptyChecklistArePending() {
        assertThat(calculator.calculate(List.of(TaskStatus.TODO, TaskStatus.TODO))).isEqualTo(EmployeeStatus.PENDING);
        assertThat(calculator.calculate(List.of())).isEqualTo(EmployeeStatus.PENDING);
    }

    @Test
    void anyStartedOrCompletedTaskMeansOnboardingUntilAllAreDone() {
        assertThat(calculator.calculate(List.of(TaskStatus.IN_PROGRESS, TaskStatus.TODO)))
                .isEqualTo(EmployeeStatus.ONBOARDING);
        assertThat(calculator.calculate(List.of(TaskStatus.DONE, TaskStatus.TODO)))
                .isEqualTo(EmployeeStatus.ONBOARDING);
    }

    @Test
    void allTasksDoneMeansActive() {
        assertThat(calculator.calculate(List.of(TaskStatus.DONE, TaskStatus.DONE))).isEqualTo(EmployeeStatus.ACTIVE);
    }
}
