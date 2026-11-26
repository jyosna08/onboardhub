package com.onboardhub.employee.service.impl;

import com.onboardhub.employee.dto.CreateEmployeeRequest;
import com.onboardhub.employee.dto.DashboardResponse;
import com.onboardhub.employee.dto.EmployeeDetailResponse;
import com.onboardhub.employee.dto.EmployeeSummaryResponse;
import com.onboardhub.employee.dto.TaskResponse;
import com.onboardhub.employee.dto.UpdateEmployeeRequest;
import com.onboardhub.employee.dto.UpdateTaskStatusRequest;
import com.onboardhub.employee.entity.Employee;
import com.onboardhub.employee.entity.OnboardingTask;
import com.onboardhub.employee.enums.Department;
import com.onboardhub.employee.enums.EmployeeStatus;
import com.onboardhub.employee.enums.OwnerRole;
import com.onboardhub.employee.enums.Role;
import com.onboardhub.employee.enums.TaskCategory;
import com.onboardhub.employee.enums.TaskStatus;
import com.onboardhub.employee.exception.DuplicateEmailException;
import com.onboardhub.employee.exception.InvalidTaskTransitionException;
import com.onboardhub.employee.exception.ResourceNotFoundException;
import com.onboardhub.employee.mapper.EmployeeMapper;
import com.onboardhub.employee.repository.EmployeeRepository;
import com.onboardhub.employee.repository.EmployeeSpecifications;
import com.onboardhub.employee.repository.EmployeeTaskCounts;
import com.onboardhub.employee.repository.OnboardingTaskRepository;
import com.onboardhub.employee.security.AuthenticatedUser;
import com.onboardhub.employee.service.ChecklistFactory;
import com.onboardhub.employee.service.EmployeeService;
import com.onboardhub.employee.service.EmployeeStatusCalculator;
import com.onboardhub.employee.service.EmployeeStatusUpdater;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeServiceImpl implements EmployeeService {
    private final EmployeeRepository employees;
    private final OnboardingTaskRepository tasks;
    private final ChecklistFactory checklistFactory;
    private final EmployeeMapper mapper;
    private final EmployeeStatusUpdater statusUpdater;
    private final Clock clock;

    public EmployeeServiceImpl(EmployeeRepository employees, OnboardingTaskRepository tasks,
            ChecklistFactory checklistFactory, EmployeeMapper mapper,
            EmployeeStatusCalculator statusCalculator, Clock clock) {
        this.employees = employees;
        this.tasks = tasks;
        this.checklistFactory = checklistFactory;
        this.mapper = mapper;
        this.statusUpdater = new EmployeeStatusUpdater(statusCalculator);
        this.clock = clock;
    }

    @Override
    @Transactional
    public EmployeeDetailResponse createEmployee(CreateEmployeeRequest request, AuthenticatedUser caller) {
        List<com.onboardhub.employee.enums.AssetType> types = checklistFactory.normalizeAssetTypes(request.requiredAssetTypes());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (employees.existsByEmailIgnoreCase(email)) throw new DuplicateEmailException("Email already exists");
        return persistEmployee(request, caller, email, types);
    }

    private EmployeeDetailResponse persistEmployee(CreateEmployeeRequest request, AuthenticatedUser caller,
            String email, List<com.onboardhub.employee.enums.AssetType> types) {
        try {
            Employee employee = newEmployee(request, caller, email);
            employee = employees.saveAndFlush(employee);
            employee.setEmployeeCode(employeeCode(employee.getId()));
            employee.setUpdatedAt(LocalDateTime.now(clock));
            employee = employees.saveAndFlush(employee);
            List<OnboardingTask> createdTasks = tasks.saveAll(checklistFactory.createTasks(employee, types));
            return mapper.toDetail(employee, createdTasks);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException("Email already exists");
        }
    }

    private Employee newEmployee(CreateEmployeeRequest request, AuthenticatedUser caller, String email) {
        Employee employee = new Employee(request.fullName().trim(), email, request.department(),
                request.jobTitle().trim(), request.joiningDate(), caller.fullName());
        employee.initializeTimestamps(LocalDateTime.now(clock));
        return employee;
    }

    private String employeeCode(Long id) {
        return "EMP-" + String.format(Locale.ROOT, "%04d", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSummaryResponse> listEmployees(EmployeeStatus status, Department department, String search) {
        List<Employee> results = employees.findAll(EmployeeSpecifications.matching(status, department, search),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return summariesWithTaskCounts(results);
    }

    private List<EmployeeSummaryResponse> summariesWithTaskCounts(List<Employee> results) {
        if (results.isEmpty()) return List.of();
        List<Long> ids = results.stream().map(Employee::getId).toList();
        Map<Long, EmployeeTaskCounts> counts = tasks.countTasksByEmployeeIds(ids).stream()
                .collect(Collectors.toMap(EmployeeTaskCounts::getEmployeeId, Function.identity()));
        return results.stream().map(employee -> mapper.toSummary(employee, counts.get(employee.getId()))).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDetailResponse getEmployee(Long id, AuthenticatedUser caller) {
        Employee employee = requireEmployee(id);
        enforceOwnRecord(employee, caller);
        return detail(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDetailResponse getMyEmployee(AuthenticatedUser caller) {
        Employee employee = employees.findByEmailIgnoreCase(normalizeEmail(caller.email()))
                .orElseThrow(() -> new ResourceNotFoundException("Employee record not found"));
        return detail(employee);
    }

    private void enforceOwnRecord(Employee employee, AuthenticatedUser caller) {
        if (caller.role() == Role.EMPLOYEE && !employee.getEmail().equalsIgnoreCase(caller.email())) {
            throw new ResourceNotFoundException("Employee not found");
        }
    }

    private EmployeeDetailResponse detail(Employee employee) {
        return mapper.toDetail(employee, tasks.findByEmployee_IdOrderBySortOrderAsc(employee.getId()));
    }

    @Override
    @Transactional
    public EmployeeDetailResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = requireEmployee(id);
        applyEmployeeChanges(employee, request);
        employee.setUpdatedAt(LocalDateTime.now(clock));
        employees.save(employee);
        return detail(employee);
    }

    private void applyEmployeeChanges(Employee employee, UpdateEmployeeRequest request) {
        if (request.fullName() != null) employee.setFullName(request.fullName().trim());
        if (request.department() != null) employee.setDepartment(request.department());
        if (request.jobTitle() != null) employee.setJobTitle(request.jobTitle().trim());
        if (request.joiningDate() != null) employee.setJoiningDate(request.joiningDate());
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, UpdateTaskStatusRequest request, AuthenticatedUser caller) {
        OnboardingTask task = tasks.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        authorizeTaskOwner(task, caller);
        validateTransition(task, request.status());
        updateTask(task, request.status(), caller.fullName());
        tasks.save(task);
        recomputeEmployeeStatus(task.getEmployee());
        return mapper.toTask(task);
    }

    private void authorizeTaskOwner(OnboardingTask task, AuthenticatedUser caller) {
        boolean admin = caller.role() == Role.ADMIN;
        boolean owner = caller.role().name().equals(task.getOwnerRole().name());
        if (!admin && !owner) throw new AccessDeniedException("Task belongs to " + task.getOwnerRole());
    }

    private void validateTransition(OnboardingTask task, TaskStatus next) {
        if (task.getStatus() == TaskStatus.DONE) throw new InvalidTaskTransitionException("DONE tasks cannot be changed");
        if (task.getCategory() == TaskCategory.EQUIPMENT && next == TaskStatus.DONE) {
            throw new InvalidTaskTransitionException("Equipment tasks are completed by assigning an asset");
        }
        if (!isAllowed(task.getStatus(), next)) throw new InvalidTaskTransitionException("Invalid task status transition");
    }

    private boolean isAllowed(TaskStatus current, TaskStatus next) {
        return (current == TaskStatus.TODO && next == TaskStatus.IN_PROGRESS)
                || (current == TaskStatus.IN_PROGRESS && next == TaskStatus.TODO)
                || ((current == TaskStatus.TODO || current == TaskStatus.IN_PROGRESS) && next == TaskStatus.DONE);
    }

    private void updateTask(OnboardingTask task, TaskStatus next, String completedBy) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (next == TaskStatus.DONE) task.markCompleted(now, completedBy);
        else {
            task.setStatus(next);
            task.clearCompletion();
        }
        task.setUpdatedAt(now);
    }

    private void recomputeEmployeeStatus(Employee employee) {
        List<TaskStatus> statuses = tasks.findByEmployee_IdOrderBySortOrderAsc(employee.getId()).stream()
                .map(OnboardingTask::getStatus).toList();
        statusUpdater.update(employee, statuses, LocalDateTime.now(clock));
        employees.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        long pending = employees.countByStatus(EmployeeStatus.PENDING);
        long onboarding = employees.countByStatus(EmployeeStatus.ONBOARDING);
        long active = employees.countByStatus(EmployeeStatus.ACTIVE);
        return new DashboardResponse(pending, onboarding, active, tasks.countByStatusNot(TaskStatus.DONE),
                tasks.countOverdueOpenTasks(LocalDate.now(clock)));
    }

    private Employee requireEmployee(Long id) {
        return employees.findById(id).orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
