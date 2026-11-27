package com.onboardhub.employee.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onboardhub.employee.client.AssetClient;
import com.onboardhub.employee.client.AssetRemoteResponse;
import com.onboardhub.employee.entity.Employee;
import com.onboardhub.employee.entity.OnboardingTask;
import com.onboardhub.employee.enums.AssetType;
import com.onboardhub.employee.enums.Department;
import com.onboardhub.employee.enums.EmployeeStatus;
import com.onboardhub.employee.enums.TaskCategory;
import com.onboardhub.employee.enums.TaskStatus;
import com.onboardhub.employee.exception.AssetConflictException;
import com.onboardhub.employee.exception.AssetServiceUnavailableException;
import com.onboardhub.employee.exception.ResourceNotFoundException;
import com.onboardhub.employee.repository.EmployeeRepository;
import com.onboardhub.employee.repository.OnboardingTaskRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeControllerIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EmployeeRepository employees;
    @Autowired private OnboardingTaskRepository tasks;
    @MockBean private AssetClient assetClient;
    @Value("${app.jwt.secret}") private String jwtSecret;

    @BeforeEach
    void clearDatabase() {
        tasks.deleteAll();
        employees.deleteAll();
    }

    @Test
    void contextLoads() {
        assertThat(employees).isNotNull();
        assertThat(tasks).isNotNull();
    }

    @Test
    void hrCreatesEmployeeChecklistAndValidationAndRolesAreEnforced() throws Exception {
        MvcResult created = create("Asha Rao", "asha@example.com", "ENGINEERING", "2026-12-02",
                "[\"LAPTOP\",\"MONITOR\"]", "HR").andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeCode").value(org.hamcrest.Matchers.matchesPattern("EMP-\\d{4,}")))
                .andExpect(jsonPath("$.tasks.length()").value(7))
                .andExpect(jsonPath("$.tasks[0].title").value("Collect identity and joining documents"))
                .andExpect(jsonPath("$.tasks[4].title").value("Assign LAPTOP"))
                .andExpect(jsonPath("$.tasks[5].title").value("Assign MONITOR"))
                .andExpect(jsonPath("$.tasks[6].title").value("Orientation session"))
                .andExpect(jsonPath("$.tasks[0].dueDate").value("2026-11-30"))
                .andReturn();
        JsonNode employee = objectMapper.readTree(created.getResponse().getContentAsString());
        assertThat(employee.get("tasks").get(0).get("ownerRole").asText()).isEqualTo("HR");
        assertThat(employee.get("tasks").get(2).get("category").asText()).isEqualTo("ACCOUNT_SETUP");
        create("Asha Rao", "asha@example.com", "ENGINEERING", "2026-12-02", "null", "HR")
                .andExpect(status().isConflict());
        mvc.perform(post("/api/employees").header("Authorization", bearer("it@example.com", "IT"))
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJson("it@example.com", "[]")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/employees").header("Authorization", bearer("person@example.com", "EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJson("person@example.com", "[]")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/employees").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api/employees").header("Authorization", bearer("hr@example.com", "HR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/employees").header("Authorization", bearer("hr@example.com", "HR"))
                        .contentType(MediaType.APPLICATION_JSON).content(employeeJsonWithUnknownType()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listFiltersStatusDepartmentSearchAndGroupedTaskCounts() throws Exception {
        long pending = createAndReadId("Pending Person", "pending@example.com", "ENGINEERING", "HR", "[]");
        long onboarding = createAndReadId("Onboard Person", "onboard@example.com", "FINANCE", "HR", "[]");
        long taskId = taskId(onboarding, 1);
        updateTask(taskId, "IN_PROGRESS", "HR").andExpect(status().isOk());
        mvc.perform(get("/api/employees").param("status", "PENDING").header("Authorization", bearer("hr@example.com", "HR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(pending))
                .andExpect(jsonPath("$[0].tasksDone").value(0)).andExpect(jsonPath("$[0].tasksTotal").value(6));
        mvc.perform(get("/api/employees").param("department", "FINANCE").param("search", "ONBOARD")
                        .header("Authorization", bearer("it@example.com", "IT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(onboarding));
        mvc.perform(get("/api/employees").param("status", "ONBOARDING").param("department", "FINANCE")
                        .param("search", "onboard@example").header("Authorization", bearer("admin@example.com", "ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/employees").param("status", "INVALID")
                        .header("Authorization", bearer("hr@example.com", "HR")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void employeeOwnRecordMeAndPatchRulesWork() throws Exception {
        long ownId = createAndReadId("Asha Rao", "asha@example.com", "ENGINEERING", "HR", "[]");
        long otherId = createAndReadId("Other Person", "other@example.com", "SALES", "HR", "[]");
        String employeeToken = bearer("ASHA@example.com", "EMPLOYEE");
        mvc.perform(get("/api/employees/{id}", ownId).header("Authorization", employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("asha@example.com"));
        mvc.perform(get("/api/employees/{id}", otherId).header("Authorization", employeeToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/employees/me").header("Authorization", employeeToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ownId));
        mvc.perform(get("/api/employees/me").header("Authorization", bearer("no-record@example.com", "EMPLOYEE")))
                .andExpect(status().isNotFound());
        LocalDate oldDueDate = tasks.findByEmployee_IdOrderBySortOrderAsc(ownId).get(0).getDueDate();
        mvc.perform(patch("/api/employees/{id}", ownId).header("Authorization", bearer("hr@example.com", "HR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Asha Updated\",\"department\":\"FINANCE\","
                                + "\"jobTitle\":\"Architect\",\"joiningDate\":\"2027-01-01\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Asha Updated"))
                .andExpect(jsonPath("$.email").value("asha@example.com"));
        assertThat(tasks.findByEmployee_IdOrderBySortOrderAsc(ownId).get(0).getDueDate()).isEqualTo(oldDueDate);
        mvc.perform(patch("/api/employees/{id}", ownId).header("Authorization", bearer("it@example.com", "IT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void taskOwnershipStatusTransitionsEquipmentRuleAndDashboardWork() throws Exception {
        long employeeId = createAndReadId("Flow Person", "flow@example.com", "ENGINEERING", "HR",
                "[\"LAPTOP\",\"MONITOR\"]");
        var employeeTasks = tasks.findByEmployee_IdOrderBySortOrderAsc(employeeId);
        long hrTask = employeeTasks.get(0).getId();
        long itTask = employeeTasks.get(2).getId();
        long equipmentTask = employeeTasks.get(4).getId();
        updateTask(hrTask, "IN_PROGRESS", "IT").andExpect(status().isForbidden());
        updateTask(hrTask, "IN_PROGRESS", "HR").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(get("/api/employees/{id}", employeeId).header("Authorization", bearer("flow@example.com", "EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ONBOARDING"));
        updateTask(itTask, "IN_PROGRESS", "HR").andExpect(status().isForbidden());
        updateTask(itTask, "IN_PROGRESS", "IT").andExpect(status().isOk());
        updateTask(equipmentTask, "DONE", "IT").andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Equipment tasks are completed by assigning an asset"));
        updateTask(hrTask, "DONE", "HR").andExpect(status().isOk());
        updateTask(hrTask, "TODO", "HR").andExpect(status().isConflict());

        completeNonEquipmentTasks(employeeTasks, hrTask, employeeTasks.get(6).getId());
        completeEquipmentThroughRepository(employeeTasks);
        updateTask(employeeTasks.get(6).getId(), "DONE", "HR").andExpect(status().isOk());
        mvc.perform(get("/api/employees/{id}", employeeId).header("Authorization", bearer("flow@example.com", "EMPLOYEE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.onboardingCompletedAt").isNotEmpty());

        createAndReadId("Pending", "pending-dash@example.com", "HR", "HR", "[]");
        long onboardingId = createAndReadId("Onboarding", "onboarding-dash@example.com", "IT", "HR", "[]");
        updateTask(taskId(onboardingId, 3), "IN_PROGRESS", "IT").andExpect(status().isOk());
        mvc.perform(get("/api/onboarding/dashboard").header("Authorization", bearer("hr@example.com", "HR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pendingEmployees").value(1))
                .andExpect(jsonPath("$.onboardingEmployees").value(1)).andExpect(jsonPath("$.activeEmployees").value(1))
                .andExpect(jsonPath("$.openTasks").value(12)).andExpect(jsonPath("$.overdueTasks").value(12));
        mvc.perform(get("/api/onboarding/dashboard").header("Authorization", bearer("person@example.com", "EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignmentEndpointCompletesEquipmentTaskAndEnforcesPermissionsAndErrors() throws Exception {
        long employeeId = createAndReadId("Asset Person", "asset-person@example.com", "ENGINEERING", "HR",
                "[\"LAPTOP\",\"MONITOR\"]");
        java.util.List<OnboardingTask> employeeTasks = tasks.findByEmployee_IdOrderBySortOrderAsc(employeeId);
        java.util.List<OnboardingTask> equipmentTasks = employeeTasks.stream()
                .filter(task -> task.getCategory() == TaskCategory.EQUIPMENT).toList();
        OnboardingTask equipment = equipmentTasks.get(0);
        when(assetClient.assign(any(), any())).thenReturn(new AssetRemoteResponse(7L, "LAP-0007", AssetType.LAPTOP,
                "Dell", "Latitude 5440", "serial", "ASSIGNED"));
        mvc.perform(post("/api/onboarding/tasks/{taskId}/assign-asset", equipment.getId())
                        .header("Authorization", bearer("it@example.com", "IT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":7}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.assetTag").value("LAP-0007"))
                .andExpect(jsonPath("$.assetLabel").value("Dell Latitude 5440"))
                .andExpect(jsonPath("$.completedByName").value("Test IT"));
        assertThat(employees.findById(employeeId).orElseThrow().getStatus()).isEqualTo(EmployeeStatus.ONBOARDING);
        mvc.perform(post("/api/onboarding/tasks/{taskId}/assign-asset", equipment.getId())
                        .header("Authorization", bearer("it@example.com", "IT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":7}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("This task is already completed"));
        mvc.perform(post("/api/onboarding/tasks/{taskId}/assign-asset", equipment.getId())
                        .header("Authorization", bearer("hr@example.com", "HR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":7}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/onboarding/tasks/{taskId}/assign-asset", equipment.getId())
                        .header("Authorization", bearer("person@example.com", "EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":7}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/onboarding/tasks/{taskId}/assign-asset", equipment.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":7}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/onboarding/tasks/{taskId}/assign-asset", equipment.getId())
                        .header("Authorization", bearer("it@example.com", "IT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        OnboardingTask documentation = employeeTasks.stream()
                .filter(task -> task.getCategory() != TaskCategory.EQUIPMENT).findFirst().orElseThrow();
        mvc.perform(post("/api/onboarding/tasks/{taskId}/assign-asset", documentation.getId())
                        .header("Authorization", bearer("it@example.com", "IT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":8}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This task is not an equipment task"));
        when(assetClient.assign(any(), any())).thenReturn(new AssetRemoteResponse(8L, "MON-0008", AssetType.MONITOR,
                "Dell", "P2422H", "serial2", "ASSIGNED"));
        mvc.perform(post("/api/onboarding/tasks/{taskId}/assign-asset", equipmentTasks.get(1).getId())
                        .header("Authorization", bearer("it@example.com", "IT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":8}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DONE"));
        for (OnboardingTask openTask : employeeTasks) {
            if (openTask.getCategory() != TaskCategory.EQUIPMENT) {
                updateTask(openTask.getId(), "DONE", openTask.getOwnerRole().name()).andExpect(status().isOk());
            }
        }
        assertThat(employees.findById(employeeId).orElseThrow().getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void remoteAssignmentErrorsLeaveTaskTodo() throws Exception {
        long employeeId = createAndReadId("Asset Error", "asset-error@example.com", "ENGINEERING", "HR",
                "[\"LAPTOP\"]");
        OnboardingTask equipment = tasks.findByEmployee_IdOrderBySortOrderAsc(employeeId).stream()
                .filter(task -> task.getCategory() == TaskCategory.EQUIPMENT).findFirst().orElseThrow();
        doThrow(new AssetConflictException("Asset unavailable")).when(assetClient).assign(any(), any());
        mvc.perform(post("/api/onboarding/tasks/{taskId}/assign-asset", equipment.getId())
                        .header("Authorization", bearer("it@example.com", "IT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":7}"))
                .andExpect(status().isConflict());
        assertThat(tasks.findById(equipment.getId()).orElseThrow().getStatus()).isEqualTo(TaskStatus.TODO);
        doThrow(new ResourceNotFoundException("Asset not found")).when(assetClient).assign(any(), any());
        mvc.perform(post("/api/onboarding/tasks/{taskId}/assign-asset", equipment.getId())
                        .header("Authorization", bearer("it@example.com", "IT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":7}"))
                .andExpect(status().isNotFound());
        doThrow(new AssetServiceUnavailableException()).when(assetClient).assign(any(), any());
        mvc.perform(post("/api/onboarding/tasks/{taskId}/assign-asset", equipment.getId())
                        .header("Authorization", bearer("it@example.com", "IT"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"assetId\":7}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(tasks.findById(equipment.getId()).orElseThrow().getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void swaggerAndHealthArePublicAndProtectedRoutesRequireTokens() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/onboarding/dashboard")).andExpect(status().isUnauthorized());
    }

    private org.springframework.test.web.servlet.ResultActions create(String name, String email, String department,
            String joiningDate, String requiredTypes, String role) throws Exception {
        String body = "{\"fullName\":\"" + name + "\",\"email\":\"" + email + "\",\"department\":\""
                + department + "\",\"jobTitle\":\"Engineer\",\"joiningDate\":\"" + joiningDate
                + "\",\"requiredAssetTypes\":" + requiredTypes + "}";
        return mvc.perform(post("/api/employees").header("Authorization", bearer(email, role))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createAndReadId(String name, String email, String department, String role, String types) throws Exception {
        MvcResult result = create(name, email, department, LocalDate.now(Clock.systemUTC()).minusDays(30).toString(), types, role)
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long taskId(long employeeId, int sortOrder) {
        return tasks.findByEmployee_IdOrderBySortOrderAsc(employeeId).get(sortOrder - 1).getId();
    }

    private org.springframework.test.web.servlet.ResultActions updateTask(long taskId, String status, String role)
            throws Exception {
        return mvc.perform(patch("/api/onboarding/tasks/{taskId}/status", taskId)
                .header("Authorization", bearer(role.toLowerCase(Locale.ROOT) + "@example.com", role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    private void completeNonEquipmentTasks(java.util.List<OnboardingTask> employeeTasks, long alreadyDoneHrTask,
            long finalOrientationTask) throws Exception {
        for (OnboardingTask task : employeeTasks) {
            if (task.getCategory() == TaskCategory.EQUIPMENT || task.getId().equals(alreadyDoneHrTask)
                    || task.getId().equals(finalOrientationTask)) continue;
            String role = task.getOwnerRole().name();
            updateTask(task.getId(), "DONE", role).andExpect(status().isOk());
        }
    }

    private void completeEquipmentThroughRepository(java.util.List<OnboardingTask> employeeTasks) {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        employeeTasks.stream().filter(task -> task.getCategory() == TaskCategory.EQUIPMENT)
                .forEach(task -> task.markCompleted(now, "Asset assignment"));
        tasks.saveAll(employeeTasks.stream().filter(task -> task.getCategory() == TaskCategory.EQUIPMENT).toList());
    }

    private String bearer(String email, String role) {
        Instant now = Instant.now();
        String token = Jwts.builder().subject("23").claim("email", email.toLowerCase(Locale.ROOT))
                .claim("role", role).claim("fullName", "Test " + role).issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256).compact();
        return "Bearer " + token;
    }

    private String employeeJson(String email, String types) {
        return "{\"fullName\":\"Test\",\"email\":\"" + email + "\",\"department\":\"HR\","
                + "\"jobTitle\":\"Engineer\",\"joiningDate\":\"2026-12-01\",\"requiredAssetTypes\":" + types + "}";
    }

    private String employeeJsonWithUnknownType() {
        return "{\"fullName\":\"Test\",\"email\":\"unknown-type@example.com\",\"department\":\"HR\","
                + "\"jobTitle\":\"Engineer\",\"joiningDate\":\"2026-12-01\","
                + "\"requiredAssetTypes\":[\"PRINTER\"]}";
    }
}
