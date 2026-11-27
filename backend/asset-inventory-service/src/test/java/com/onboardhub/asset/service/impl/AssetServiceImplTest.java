package com.onboardhub.asset.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.onboardhub.asset.dto.AssignAssetRequest;
import com.onboardhub.asset.dto.CreateAssetRequest;
import com.onboardhub.asset.dto.UpdateStatusRequest;
import com.onboardhub.asset.entity.Asset;
import com.onboardhub.asset.entity.AssetAssignment;
import com.onboardhub.asset.enums.AssetStatus;
import com.onboardhub.asset.enums.AssetType;
import com.onboardhub.asset.enums.Role;
import com.onboardhub.asset.exception.AssetConflictException;
import com.onboardhub.asset.exception.DuplicateSerialException;
import com.onboardhub.asset.mapper.AssetMapper;
import com.onboardhub.asset.repository.AssetAssignmentRepository;
import com.onboardhub.asset.repository.AssetCountProjection;
import com.onboardhub.asset.repository.AssetRepository;
import com.onboardhub.asset.security.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;

class AssetServiceImplTest {
    private AssetRepository assets;
    private AssetAssignmentRepository assignments;
    private AssetServiceImpl service;

    @BeforeEach
    void setUp() {
        assets = mock(AssetRepository.class);
        assignments = mock(AssetAssignmentRepository.class);
        service = new AssetServiceImpl(assets, assignments, new AssetMapper(),
                Clock.fixed(Instant.parse("2026-01-02T03:04:05Z"), ZoneOffset.UTC));
    }

    @ParameterizedTest
    @CsvSource({"LAPTOP,LAP", "MONITOR,MON", "KEYBOARD,KEY", "MOUSE,MOU", "HEADSET,HEA", "MOBILE,MOB"})
    void generatesTagForEachAssetType(AssetType type, String prefix) {
        when(assets.existsBySerialNumber("SER-1")).thenReturn(false);
        when(assets.saveAndFlush(any(Asset.class))).thenAnswer(call -> identify(call.getArgument(0), 7L));
        var response = service.createAsset(createRequest(type, " SER-1 "));
        assertThat(response.assetTag()).isEqualTo(prefix + "-0007");
        assertThat(response.status()).isEqualTo(AssetStatus.AVAILABLE);
        assertThat(response.serialNumber()).isEqualTo("SER-1");
    }

    @Test
    void duplicateSerialIsRejectedCaseInsensitivelyAfterTrimming() {
        when(assets.existsBySerialNumber("SER-1")).thenReturn(true);
        assertThatThrownBy(() -> service.createAsset(createRequest(AssetType.LAPTOP, " SER-1 ")))
                .isInstanceOf(DuplicateSerialException.class);
    }

    @Test
    void assignsAvailableAssetAndAddsHistory() {
        Asset available = asset(3L, AssetType.LAPTOP, AssetStatus.AVAILABLE);
        Asset assigned = asset(3L, AssetType.LAPTOP, AssetStatus.ASSIGNED);
        ReflectionTestUtils.setField(assigned, "assignedEmployeeId", 19L);
        ReflectionTestUtils.setField(assigned, "assignedEmployeeName", "Asha Rao");
        ReflectionTestUtils.setField(assigned, "assignedAt", LocalDateTimeFixture.now());
        when(assets.findById(3L)).thenReturn(Optional.of(available), Optional.of(assigned));
        when(assets.assignIfAvailable(any(), any(), any(), any())).thenReturn(1);
        when(assignments.save(any(AssetAssignment.class))).thenAnswer(call -> call.getArgument(0));
        var response = service.assignAsset(3L, new AssignAssetRequest(19L, "Asha Rao", AssetType.LAPTOP), caller());
        assertThat(response.status()).isEqualTo(AssetStatus.ASSIGNED);
        assertThat(response.assignedEmployeeId()).isEqualTo(19L);
    }

    @Test
    void wrongExpectedTypeIsConflict() {
        when(assets.findById(3L)).thenReturn(Optional.of(asset(3L, AssetType.MONITOR, AssetStatus.AVAILABLE)));
        assertThatThrownBy(() -> service.assignAsset(3L,
                new AssignAssetRequest(19L, "Asha Rao", AssetType.LAPTOP), caller()))
                .isInstanceOf(AssetConflictException.class)
                .hasMessage("Asset type does not match: expected LAPTOP but the asset is MONITOR");
    }

    @Test
    void unavailableAssetIsConflict() {
        when(assets.findById(3L)).thenReturn(Optional.of(asset(3L, AssetType.LAPTOP, AssetStatus.AVAILABLE)));
        when(assets.assignIfAvailable(any(), any(), any(), any())).thenReturn(0);
        assertThatThrownBy(() -> service.assignAsset(3L,
                new AssignAssetRequest(19L, "Asha Rao", AssetType.LAPTOP), caller()))
                .isInstanceOf(AssetConflictException.class).hasMessage("Asset LAP-0003 is not available");
    }

    @Test
    void returnReleasesAssetAndClosesHistory() {
        when(assets.findById(3L)).thenReturn(
                Optional.of(asset(3L, AssetType.LAPTOP, AssetStatus.ASSIGNED)),
                Optional.of(asset(3L, AssetType.LAPTOP, AssetStatus.AVAILABLE)));
        when(assets.releaseIfAssigned(any(), any())).thenReturn(1);
        AssetAssignment open = new AssetAssignment(3L, 19L, "Asha Rao", "Ravi IT", LocalDateTimeFixture.now());
        when(assignments.findFirstByAssetIdAndReturnedAtIsNullOrderByAssignedAtDesc(3L)).thenReturn(Optional.of(open));
        when(assignments.save(open)).thenReturn(open);
        assertThat(service.returnAsset(3L, caller()).status()).isEqualTo(AssetStatus.AVAILABLE);
        assertThat(open.getReturnedAt()).isEqualTo(LocalDateTimeFixture.now());
        assertThat(open.getReturnedByName()).isEqualTo("Ravi IT");
    }

    @Test
    void returningNonAssignedAssetIsConflict() {
        when(assets.findById(3L)).thenReturn(Optional.of(asset(3L, AssetType.LAPTOP, AssetStatus.AVAILABLE)));
        when(assets.releaseIfAssigned(any(), any())).thenReturn(0);
        assertThatThrownBy(() -> service.returnAsset(3L, caller())).isInstanceOf(AssetConflictException.class)
                .hasMessage("Asset LAP-0003 is not assigned");
    }

    @ParameterizedTest
    @EnumSource(value = AssetStatus.class, names = {"AVAILABLE", "IN_REPAIR", "RETIRED"})
    void updateStatusAllowsNonAssignedStatuses(AssetStatus status) {
        Asset asset = asset(3L, AssetType.LAPTOP, AssetStatus.AVAILABLE);
        when(assets.findById(3L)).thenReturn(Optional.of(asset));
        when(assets.save(asset)).thenReturn(asset);
        assertThat(service.updateStatus(3L, new UpdateStatusRequest(status)).status()).isEqualTo(status);
    }

    @Test
    void updateStatusRejectsAssignedAssetAndManualAssignment() {
        when(assets.findById(3L)).thenReturn(Optional.of(asset(3L, AssetType.LAPTOP, AssetStatus.ASSIGNED)));
        assertThatThrownBy(() -> service.updateStatus(3L, new UpdateStatusRequest(AssetStatus.AVAILABLE)))
                .isInstanceOf(AssetConflictException.class)
                .hasMessage("Return the asset before changing its status");
        assertThatThrownBy(() -> service.updateStatus(3L, new UpdateStatusRequest(AssetStatus.ASSIGNED)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void summaryIncludesEveryTypeInEnumOrderWithZeros() {
        when(assets.countByTypeAndStatus()).thenReturn(new ArrayList<>());
        var summary = service.summary();
        assertThat(summary.total()).isZero();
        assertThat(summary.byType()).extracting("type").containsExactly(AssetType.values());
        assertThat(summary.byType()).allSatisfy(type -> {
            assertThat(type.available()).isZero();
            assertThat(type.assigned()).isZero();
            assertThat(type.inRepair()).isZero();
            assertThat(type.retired()).isZero();
        });
    }

    private CreateAssetRequest createRequest(AssetType type, String serial) {
        return new CreateAssetRequest(type, " Dell ", " Latitude ", serial, LocalDate.of(2025, 1, 1));
    }

    private Asset asset(Long id, AssetType type, AssetStatus status) {
        Asset asset = new Asset(type, "Dell", "Latitude", "SER-" + id, null);
        identify(asset, id);
        asset.setAssetTag((type == AssetType.LAPTOP ? "LAP" : "MON") + "-" + String.format("%04d", id));
        asset.setStatus(status);
        asset.initializeTimestamps(LocalDateTimeFixture.now());
        return asset;
    }

    private Asset identify(Asset asset, Long id) {
        ReflectionTestUtils.setField(asset, "id", id);
        return asset;
    }

    private AuthenticatedUser caller() {
        return new AuthenticatedUser(10L, "ravi@example.com", Role.IT, "Ravi IT");
    }

    private static final class LocalDateTimeFixture {
        private static java.time.LocalDateTime now() {
            return java.time.LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        }
    }
}
