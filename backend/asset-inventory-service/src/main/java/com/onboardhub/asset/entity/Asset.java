package com.onboardhub.asset.entity;

import com.onboardhub.asset.enums.AssetStatus;
import com.onboardhub.asset.enums.AssetType;
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
@Table(name = "assets", indexes = @Index(name = "idx_assets_type_status", columnList = "type,status"))
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_tag", unique = true, length = 20)
    private String assetTag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetType type;

    @Column(nullable = false, length = 60)
    private String make;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(name = "serial_number", nullable = false, unique = true, length = 80)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssetStatus status = AssetStatus.AVAILABLE;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "assigned_employee_id")
    private Long assignedEmployeeId;

    @Column(name = "assigned_employee_name", length = 100)
    private String assignedEmployeeName;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Asset() {
    }

    public Asset(AssetType type, String make, String model, String serialNumber, LocalDate purchaseDate) {
        this.type = type;
        this.make = make;
        this.model = model;
        this.serialNumber = serialNumber;
        this.purchaseDate = purchaseDate;
    }

    public void initializeTimestamps(LocalDateTime now) {
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void setAssetTag(String assetTag) { this.assetTag = assetTag; }
    public void setStatus(AssetStatus status) { this.status = status; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getId() { return id; }
    public String getAssetTag() { return assetTag; }
    public AssetType getType() { return type; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public String getSerialNumber() { return serialNumber; }
    public AssetStatus getStatus() { return status; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public Long getAssignedEmployeeId() { return assignedEmployeeId; }
    public String getAssignedEmployeeName() { return assignedEmployeeName; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
