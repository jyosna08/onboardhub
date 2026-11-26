package com.onboardhub.employee.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.onboardhub.employee.dto.CreateEmployeeRequest;
import com.onboardhub.employee.dto.UpdateEmployeeRequest;
import com.onboardhub.employee.dto.UpdateTaskStatusRequest;
import com.onboardhub.employee.entity.Employee;
import com.onboardhub.employee.entity.OnboardingTask;
import com.onboardhub.employee.enums.AssetType;
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
import com.onboardhub.employee.repository.EmployeeTaskCounts;
import com.onboardhub.employee.repository.OnboardingTaskRepository;
import com.onboardhub.employee.security.AuthenticatedUser;
import com.onboardhub.employee.service.ChecklistFactory;
import com.onboardhub.employee.service.EmployeeStatusCalculator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class EmployeeServiceImplTest {
    private EmployeeRepository employees;
    private OnboardingTaskRepository tasks;
    private EmployeeServiceImpl service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-02T03:04:05Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        employees = mock(EmployeeRepository.class);
        tasks = mock(OnboardingTaskRepository.class);
        service = new EmployeeServiceImpl(employees, tasks, new ChecklistFactory(clock),
                new EmployeeMapper(clock), new EmployeeStatusCalculator(), clock);
    }

    @Test
    void createGeneratesCodeLowercaseEmailAndTasks() {
        when(employees.existsByEmailIgnoreCase("asha@example.com")).thenReturn(false);
        when(employees.saveAndFlush(any(Employee.class))).thenAnswer(call -> identify(call.getArgument(0), 12L));
        when(tasks.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
        var response = service.createEmployee(createRequest(" ASHA@EXAMPLE.COM ", List.of(AssetType.LAPTOP)), caller(Role.HR));
        assertThat(response.employeeCode()).isEqualTo("EMP-0012");
        assertThat(response.email()).isEqualTo("asha@example.com");
        assertThat(response.tasks()).hasSize(6);
        assertThat(response.tasks().get(0).overdue()).isTrue();
    }

    @Test
    void duplicateEmailIsRejected() {
        when(employees.existsByEmailIgnoreCase("asha@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.createEmployee(createRequest("asha@example.com", null), caller(Role.HR)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void employeeCanReadOwnRecordButOtherEmployeeRecordIsHiddenAsNotFound() {
        Employee employee = employee(1L, "asha@example.com");
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        when(tasks.findByEmployee_IdOrderBySortOrderAsc(1L)).thenReturn(List.of());
        assertThat(service.getEmployee(1L, new AuthenticatedUser(1L, "asha@example.com", Role.EMPLOYEE, "Asha"))
                .email()).isEqualTo("asha@example.com");
        assertThatThrownBy(() -> service.getEmployee(1L,
                new AuthenticatedUser(2L, "other@example.com", Role.EMPLOYEE, "Other")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateChangesAllowedFieldsWithoutMovingExistingTaskDueDates() {
        Employee employee = employee(1L, "asha@example.com");
        OnboardingTask task = task(employee, TaskCategory.DOCUMENTATION, OwnerRole.HR, TaskStatus.TODO,
                LocalDate.of(2026, 2, 8), 1);
        when(employees.findById(1L)).thenReturn(Optional.of(employee));
        when(employees.save(employee)).thenReturn(employee);
        when(tasks.findByEmployee_IdOrderBySortOrderAsc(1L)).thenReturn(List.of(task));
        var updated = service.updateEmployee(1L, new UpdateEmployeeRequest("Asha New", Department.FINANCE,
                "Analyst", LocalDate.of(2026, 3, 1)));
        assertThat(updated.fullName()).isEqualTo("Asha New");
        assertThat(updated.email()).isEqualTo("asha@example.com");
        assertThat(updated.tasks().get(0).dueDate()).isEqualTo(LocalDate.of(2026, 2, 8));
    }

    @Test
    void taskTransitionsRecomputeEmployeeStatus() {
        Employee employee = employee(1L, "asha@example.com");
        OnboardingTask task = task(employee, TaskCategory.DOCUMENTATION, OwnerRole.HR, TaskStatus.TODO,
                LocalDate.of(2026, 1, 10), 1);
        when(tasks.findById(11L)).thenReturn(Optional.of(task));
        when(tasks.findByEmployee_IdOrderBySortOrderAsc(1L)).thenReturn(List.of(task));
        when(tasks.save(task)).thenReturn(task);
        when(employees.save(employee)).thenReturn(employee);
        var updated = service.updateTaskStatus(11L, new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS), caller(Role.HR));
        assertThat(updated.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ONBOARDING);
        service.updateTaskStatus(11L, new UpdateTaskStatusRequest(TaskStatus.TODO), caller(Role.HR));
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.PENDING);
    }

    @Test
    void doneIsFinalAndRecordsCompletionDetails() {
        Employee employee = employee(1L, "asha@example.com");
        OnboardingTask task = task(employee, TaskCategory.DOCUMENTATION, OwnerRole.HR, TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 1, 10), 1);
        when(tasks.findById(11L)).thenReturn(Optional.of(task));
        when(tasks.findByEmployee_IdOrderBySortOrderAsc(1L)).thenReturn(List.of(task));
        when(tasks.save(task)).thenReturn(task);
        when(employees.save(employee)).thenReturn(employee);
        var done = service.updateTaskStatus(11L, new UpdateTaskStatusRequest(TaskStatus.DONE), caller(Role.HR));
        assertThat(done.completedByName()).isEqualTo("Test HR");
        assertThat(done.completedAt()).isNotNull();
        assertThat(employee.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(employee.getOnboardingCompletedAt()).isNotNull();
        assertThatThrownBy(() -> service.updateTaskStatus(11L,
                new UpdateTaskStatusRequest(TaskStatus.TODO), caller(Role.HR)))
                .isInstanceOf(InvalidTaskTransitionException.class).hasMessage("DONE tasks cannot be changed");
    }

    @Test
    void equipmentCannotBeMarkedDoneManually() {
        Employee employee = employee(1L, "asha@example.com");
        OnboardingTask task = task(employee, TaskCategory.EQUIPMENT, OwnerRole.IT, TaskStatus.TODO,
                LocalDate.of(2026, 1, 10), 1);
        when(tasks.findById(11L)).thenReturn(Optional.of(task));
        assertThatThrownBy(() -> service.updateTaskStatus(11L,
                new UpdateTaskStatusRequest(TaskStatus.DONE), caller(Role.IT)))
                .isInstanceOf(InvalidTaskTransitionException.class)
                .hasMessage("Equipment tasks are completed by assigning an asset");
    }

    @Test
    void onlyTaskOwnerOrAdminCanUpdateTask() {
        Employee employee = employee(1L, "asha@example.com");
        OnboardingTask task = task(employee, TaskCategory.DOCUMENTATION, OwnerRole.HR, TaskStatus.TODO,
                LocalDate.of(2026, 1, 10), 1);
        when(tasks.findById(11L)).thenReturn(Optional.of(task));
        assertThatThrownBy(() -> service.updateTaskStatus(11L,
                new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS), caller(Role.IT)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void missingTaskAndInvalidTransitionsAreRejected() {
        when(tasks.findById(11L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateTaskStatus(11L,
                new UpdateTaskStatusRequest(TaskStatus.DONE), caller(Role.HR)))
                .isInstanceOf(ResourceNotFoundException.class);
        Employee employee = employee(1L, "asha@example.com");
        OnboardingTask task = task(employee, TaskCategory.DOCUMENTATION, OwnerRole.HR, TaskStatus.TODO,
                LocalDate.of(2026, 1, 10), 1);
        when(tasks.findById(12L)).thenReturn(Optional.of(task));
        assertThatThrownBy(() -> service.updateTaskStatus(12L,
                new UpdateTaskStatusRequest(TaskStatus.TODO), caller(Role.HR)))
                .isInstanceOf(InvalidTaskTransitionException.class);
    }

    @Test
    void dashboardUsesCountQueriesAndFixedTodayForOverdueTasks() {
        when(employees.countByStatus(EmployeeStatus.PENDING)).thenReturn(3L);
        when(employees.countByStatus(EmployeeStatus.ONBOARDING)).thenReturn(5L);
        when(employees.countByStatus(EmployeeStatus.ACTIVE)).thenReturn(20L);
        when(tasks.countByStatusNot(TaskStatus.DONE)).thenReturn(31L);
        when(tasks.countOverdueOpenTasks(LocalDate.of(2026, 1, 2))).thenReturn(4L);
        assertThat(service.dashboard()).isEqualTo(new com.onboardhub.employee.dto.DashboardResponse(3, 5, 20, 31, 4));
    }

    @Test
    void listUsesTaskCountsForReturnedEmployeeIds() {
        Employee employee = employee(1L, "asha@example.com");
        EmployeeTaskCounts count = mock(EmployeeTaskCounts.class);
        when(count.getEmployeeId()).thenReturn(1L);
        when(count.getTasksDone()).thenReturn(2L);
        when(count.getTasksTotal()).thenReturn(7L);
        when(employees.findAll(any(org.springframework.data.jpa.domain.Specification.class),
                any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(employee));
        when(tasks.countTasksByEmployeeIds(List.of(1L))).thenReturn(List.of(count));
        var result = service.listEmployees(EmployeeStatus.PENDING, Department.ENGINEERING, "asha");
        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.tasksDone()).isEqualTo(2);
            assertThat(summary.tasksTotal()).isEqualTo(7);
        });
    }

    private CreateEmployeeRequest createRequest(String email, List<AssetType> types) {
        return new CreateEmployeeRequest("Asha Rao", email, Department.ENGINEERING,
                "Backend Engineer", LocalDate.of(2026, 1, 1), types);
    }

    private Employee employee(Long id, String email) {
        Employee employee = new Employee("Asha Rao", email, Department.ENGINEERING,
                "Backend Engineer", LocalDate.of(2026, 2, 10), "Ravi HR");
        identify(employee, id);
        employee.setEmployeeCode("EMP-%04d".formatted(id));
        employee.initializeTimestamps(LocalDate.of(2026, 1, 1).atStartOfDay());
        return employee;
    }

    private Employee identify(Employee employee, Long id) {
        ReflectionTestUtils.setField(employee, "id", id);
        return employee;
    }

    private OnboardingTask task(Employee employee, TaskCategory category, OwnerRole owner,
            TaskStatus status, LocalDate dueDate, int sortOrder) {
        OnboardingTask task = new OnboardingTask(employee, "Task", category, owner, dueDate,
                sortOrder, category == TaskCategory.EQUIPMENT ? AssetType.LAPTOP : null, LocalDateTimeFixture.now());
        task.setStatus(status);
        ReflectionTestUtils.setField(task, "id", 11L);
        return task;
    }

    private AuthenticatedUser caller(Role role) {
        return new AuthenticatedUser(4L, "test@example.com", role, "Test " + role.name());
    }

    private static final class LocalDateTimeFixture {
        private static java.time.LocalDateTime now() {
            return java.time.LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        }
    }
}
