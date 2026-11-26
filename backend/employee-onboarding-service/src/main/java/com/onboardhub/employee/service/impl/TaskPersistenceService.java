package com.onboardhub.employee.service.impl;

import com.onboardhub.employee.client.AssetRemoteResponse;
import com.onboardhub.employee.dto.TaskResponse;
import com.onboardhub.employee.entity.Employee;
import com.onboardhub.employee.entity.OnboardingTask;
import com.onboardhub.employee.enums.TaskCategory;
import com.onboardhub.employee.enums.TaskStatus;
import com.onboardhub.employee.exception.AssetConflictException;
import com.onboardhub.employee.exception.ResourceNotFoundException;
import com.onboardhub.employee.mapper.EmployeeMapper;
import com.onboardhub.employee.repository.EmployeeRepository;
import com.onboardhub.employee.repository.OnboardingTaskRepository;
import com.onboardhub.employee.security.AuthenticatedUser;
import com.onboardhub.employee.service.EmployeeStatusUpdater;
import com.onboardhub.employee.service.TaskAssignmentSnapshot;
import com.onboardhub.employee.service.TaskPersistencePort;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskPersistenceService implements TaskPersistencePort {
    private final OnboardingTaskRepository tasks;
    private final EmployeeRepository employees;
    private final EmployeeMapper mapper;
    private final EmployeeStatusUpdater statusUpdater;
    private final Clock clock;

    public TaskPersistenceService(OnboardingTaskRepository tasks, EmployeeRepository employees,
            EmployeeMapper mapper, EmployeeStatusUpdater statusUpdater, Clock clock) {
        this.tasks = tasks;
        this.employees = employees;
        this.mapper = mapper;
        this.statusUpdater = statusUpdater;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskAssignmentSnapshot readAssignmentSnapshot(Long taskId) {
        OnboardingTask task = requireTask(taskId);
        return new TaskAssignmentSnapshot(task.getId(), task.getEmployee().getId(),
                task.getEmployee().getFullName(), task.getCategory().name(), task.getStatus().name(), task.getAssetType());
    }

    @Override
    @Transactional
    public TaskResponse persistAssignment(Long taskId, Long assetId, AssetRemoteResponse asset,
            AuthenticatedUser caller) {
        OnboardingTask task = requireTask(taskId);
        if (task.getStatus() == TaskStatus.DONE) {
            throw new AssetConflictException("This task is already completed");
        }
        if (task.getCategory() != TaskCategory.EQUIPMENT) {
            throw new AssetConflictException("This task is not an equipment task");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        task.assignAsset(assetId, asset.assetTag(), asset.make() + " " + asset.model(), now, caller.fullName());
        tasks.save(task);
        Employee employee = task.getEmployee();
        var statuses = tasks.findByEmployee_IdOrderBySortOrderAsc(employee.getId()).stream()
                .map(OnboardingTask::getStatus).toList();
        statusUpdater.update(employee, statuses, now);
        employees.save(employee);
        return mapper.toTask(task);
    }

    private OnboardingTask requireTask(Long taskId) {
        return tasks.findById(taskId).orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }
}
