package com.onboardhub.asset.repository;

import com.onboardhub.asset.entity.Asset;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset> {
    @Query("select case when count(a) > 0 then true else false end from Asset a "
            + "where lower(a.serialNumber) = lower(:serialNumber)")
    boolean existsBySerialNumber(@Param("serialNumber") String serialNumber);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Asset a set a.status = com.onboardhub.asset.enums.AssetStatus.ASSIGNED, "
            + "a.assignedEmployeeId = :employeeId, a.assignedEmployeeName = :employeeName, "
            + "a.assignedAt = :now, a.updatedAt = :now where a.id = :id "
            + "and a.status = com.onboardhub.asset.enums.AssetStatus.AVAILABLE")
    int assignIfAvailable(@Param("id") Long id, @Param("employeeId") Long employeeId,
            @Param("employeeName") String employeeName, @Param("now") java.time.LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Asset a set a.status = com.onboardhub.asset.enums.AssetStatus.AVAILABLE, "
            + "a.assignedEmployeeId = null, a.assignedEmployeeName = null, a.assignedAt = null, "
            + "a.updatedAt = :now where a.id = :id "
            + "and a.status = com.onboardhub.asset.enums.AssetStatus.ASSIGNED")
    int releaseIfAssigned(@Param("id") Long id, @Param("now") java.time.LocalDateTime now);

    @Query("select a.type as type, a.status as status, count(a) as count from Asset a group by a.type, a.status")
    List<AssetCountProjection> countByTypeAndStatus();
}
