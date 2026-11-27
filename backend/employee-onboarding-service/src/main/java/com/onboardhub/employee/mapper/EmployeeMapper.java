package com.onboardhub.employee.mapper;

import com.onboardhub.employee.dto.EmployeeDetailResponse;
import com.onboardhub.employee.dto.EmployeeSummaryResponse;
import com.onboardhub.employee.dto.TaskResponse;
import com.onboardhub.employee.entity.Employee;
import com.onboardhub.employee.entity.OnboardingTask;
import com.onboardhub.employee.enums.TaskStatus;
import com.onboardhub.employee.repository.EmployeeTaskCounts;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {
    private final Clock clock;

    public EmployeeMapper(Clock clock) {
        this.clock = clock;
    }

    public EmployeeSummaryResponse toSummary(Employee employee, long tasksDone, long tasksTotal) {
        return new EmployeeSummaryResponse(employee.getId(), employee.getEmployeeCode(), employee.getFullName(),
                employee.getEmail(), employee.getDepartment(), employee.getJobTitle(), employee.getJoiningDate(),
                employee.getStatus(), employee.getOnboardingCompletedAt(), employee.getCreatedAt(), tasksDone, tasksTotal);
    }

    public EmployeeSummaryResponse toSummary(Employee employee, EmployeeTaskCounts counts) {
        return toSummary(employee, counts == null ? 0 : counts.getTasksDone(),
                counts == null ? 0 : counts.getTasksTotal());
    }

    public EmployeeDetailResponse toDetail(Employee employee, List<OnboardingTask> tasks) {
        List<TaskResponse> responses = tasks.stream().map(this::toTask).toList();
        return new EmployeeDetailResponse(employee.getId(), employee.getEmployeeCode(), employee.getFullName(),
                employee.getEmail(), employee.getDepartment(), employee.getJobTitle(), employee.getJoiningDate(),
                employee.getStatus(), employee.getOnboardingCompletedAt(), employee.getCreatedAt(), responses);
    }

    public TaskResponse toTask(OnboardingTask task) {
        boolean overdue = task.getStatus() != TaskStatus.DONE && task.getDueDate().isBefore(LocalDate.now(clock));
        return new TaskResponse(task.getId(), task.getTitle(), task.getCategory(), task.getOwnerRole(),
                task.getStatus(), task.getDueDate(), overdue, task.getAssetType(), task.getAssetId(),
                task.getAssetTag(), task.getAssetLabel(), task.getCompletedAt(), task.getCompletedByName());
    }
}
