package com.onboardhub.employee.entity;

import com.onboardhub.employee.enums.AssetType;
import com.onboardhub.employee.enums.OwnerRole;
import com.onboardhub.employee.enums.TaskCategory;
import com.onboardhub.employee.enums.TaskStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "onboarding_tasks", indexes = {
        @Index(name = "idx_tasks_employee", columnList = "employee_id"),
        @Index(name = "idx_tasks_status_due", columnList = "status,due_date")})
public class OnboardingTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_role", nullable = false, length = 10)
    private OwnerRole ownerRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", length = 20)
    private AssetType assetType;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "asset_tag", length = 20)
    private String assetTag;

    @Column(name = "asset_label", length = 150)
    private String assetLabel;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by_name", length = 100)
    private String completedByName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected OnboardingTask() {
    }

    public OnboardingTask(Employee employee, String title, TaskCategory category, OwnerRole ownerRole,
            LocalDate dueDate, int sortOrder, AssetType assetType, LocalDateTime now) {
        this.employee = employee;
        this.title = title;
        this.category = category;
        this.ownerRole = ownerRole;
        this.dueDate = dueDate;
        this.sortOrder = sortOrder;
        this.assetType = assetType;
        this.status = TaskStatus.TODO;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void setStatus(TaskStatus status) { this.status = status; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void markCompleted(LocalDateTime completedAt, String completedByName) {
        this.status = TaskStatus.DONE;
        this.completedAt = completedAt;
        this.completedByName = completedByName;
    }
    public void clearCompletion() {
        this.completedAt = null;
        this.completedByName = null;
    }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public String getTitle() { return title; }
    public TaskCategory getCategory() { return category; }
    public OwnerRole getOwnerRole() { return ownerRole; }
    public TaskStatus getStatus() { return status; }
    public LocalDate getDueDate() { return dueDate; }
    public int getSortOrder() { return sortOrder; }
    public AssetType getAssetType() { return assetType; }
    public Long getAssetId() { return assetId; }
    public String getAssetTag() { return assetTag; }
    public String getAssetLabel() { return assetLabel; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public String getCompletedByName() { return completedByName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
