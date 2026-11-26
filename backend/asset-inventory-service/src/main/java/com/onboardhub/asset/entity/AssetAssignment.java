package com.onboardhub.asset.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "asset_assignments", indexes = @Index(name = "idx_assignments_asset", columnList = "asset_id"))
public class AssetAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "employee_name", nullable = false, length = 100)
    private String employeeName;

    @Column(name = "assigned_by_name", length = 100)
    private String assignedByName;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "returned_by_name", length = 100)
    private String returnedByName;

    protected AssetAssignment() {
    }

    public AssetAssignment(Long assetId, Long employeeId, String employeeName, String assignedByName,
            LocalDateTime assignedAt) {
        this.assetId = assetId;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.assignedByName = assignedByName;
        this.assignedAt = assignedAt;
    }

    public void close(LocalDateTime returnedAt, String returnedByName) {
        this.returnedAt = returnedAt;
        this.returnedByName = returnedByName;
    }

    public Long getId() { return id; }
    public Long getAssetId() { return assetId; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getAssignedByName() { return assignedByName; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public String getReturnedByName() { return returnedByName; }
}
