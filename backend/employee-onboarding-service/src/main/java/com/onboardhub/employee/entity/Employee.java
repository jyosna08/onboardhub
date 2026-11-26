package com.onboardhub.employee.entity;

import com.onboardhub.employee.enums.Department;
import com.onboardhub.employee.enums.EmployeeStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "employees", indexes = {
        @Index(name = "idx_employees_status", columnList = "status"),
        @Index(name = "idx_employees_department", columnList = "department")})
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", unique = true, length = 20)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Department department;

    @Column(name = "job_title", nullable = false, length = 100)
    private String jobTitle;

    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.PENDING;

    @Column(name = "onboarding_completed_at")
    private LocalDateTime onboardingCompletedAt;

    @Column(name = "created_by_name", length = 100)
    private String createdByName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Employee() {
    }

    public Employee(String fullName, String email, Department department, String jobTitle,
            LocalDate joiningDate, String createdByName) {
        this.fullName = fullName;
        this.email = email;
        this.department = department;
        this.jobTitle = jobTitle;
        this.joiningDate = joiningDate;
        this.createdByName = createdByName;
    }

    public void setEmployeeCode(String employeeCode) { this.employeeCode = employeeCode; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setDepartment(Department department) { this.department = department; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }
    public void setStatus(EmployeeStatus status) { this.status = status; }
    public void completeOnboarding(LocalDateTime completedAt) {
        status = EmployeeStatus.ACTIVE;
        if (onboardingCompletedAt == null) onboardingCompletedAt = completedAt;
    }
    public void markOnboardingStarted() { status = EmployeeStatus.ONBOARDING; }
    public void markPending() { status = EmployeeStatus.PENDING; }

    public Long getId() { return id; }
    public String getEmployeeCode() { return employeeCode; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public Department getDepartment() { return department; }
    public String getJobTitle() { return jobTitle; }
    public LocalDate getJoiningDate() { return joiningDate; }
    public EmployeeStatus getStatus() { return status; }
    public LocalDateTime getOnboardingCompletedAt() { return onboardingCompletedAt; }
    public String getCreatedByName() { return createdByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void initializeTimestamps(LocalDateTime now) {
        createdAt = now;
        updatedAt = now;
    }
}
