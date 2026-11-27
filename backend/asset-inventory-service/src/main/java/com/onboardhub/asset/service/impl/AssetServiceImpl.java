package com.onboardhub.asset.service.impl;

import com.onboardhub.asset.dto.AssignAssetRequest;
import com.onboardhub.asset.dto.AssetResponse;
import com.onboardhub.asset.dto.AssetSummaryResponse;
import com.onboardhub.asset.dto.AssignmentHistoryResponse;
import com.onboardhub.asset.dto.CreateAssetRequest;
import com.onboardhub.asset.dto.UpdateStatusRequest;
import com.onboardhub.asset.entity.Asset;
import com.onboardhub.asset.entity.AssetAssignment;
import com.onboardhub.asset.enums.AssetStatus;
import com.onboardhub.asset.enums.AssetType;
import com.onboardhub.asset.exception.AssetConflictException;
import com.onboardhub.asset.exception.DuplicateSerialException;
import com.onboardhub.asset.exception.ResourceNotFoundException;
import com.onboardhub.asset.mapper.AssetMapper;
import com.onboardhub.asset.repository.AssetAssignmentRepository;
import com.onboardhub.asset.repository.AssetCountProjection;
import com.onboardhub.asset.repository.AssetRepository;
import com.onboardhub.asset.repository.AssetSpecifications;
import com.onboardhub.asset.security.AuthenticatedUser;
import com.onboardhub.asset.service.AssetService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetServiceImpl implements AssetService {
    private static final Map<AssetType, String> TAG_PREFIXES = Map.of(
            AssetType.LAPTOP, "LAP", AssetType.MONITOR, "MON", AssetType.KEYBOARD, "KEY",
            AssetType.MOUSE, "MOU", AssetType.HEADSET, "HEA", AssetType.MOBILE, "MOB");
    private final AssetRepository assets;
    private final AssetAssignmentRepository assignments;
    private final AssetMapper mapper;
    private final Clock clock;

    public AssetServiceImpl(AssetRepository assets, AssetAssignmentRepository assignments,
            AssetMapper mapper, Clock clock) {
        this.assets = assets;
        this.assignments = assignments;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AssetResponse createAsset(CreateAssetRequest request) {
        String serial = request.serialNumber().trim();
        if (assets.existsBySerialNumber(serial)) throw new DuplicateSerialException("Serial number already exists");
        return saveNewAsset(request, serial);
    }

    private AssetResponse saveNewAsset(CreateAssetRequest request, String serial) {
        LocalDateTime now = LocalDateTime.now(clock);
        Asset asset = new Asset(request.type(), request.make().trim(), request.model().trim(), serial, request.purchaseDate());
        asset.initializeTimestamps(now);
        try {
            Asset saved = assets.saveAndFlush(asset);
            saved.setAssetTag(tagFor(saved.getType(), saved.getId()));
            return mapper.toResponse(assets.saveAndFlush(saved));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateSerialException("Serial number already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetResponse> listAssets(AssetType type, AssetStatus status, String search) {
        var sort = Sort.by(Sort.Direction.DESC, "createdAt");
        return assets.findAll(AssetSpecifications.matching(type, status, search), sort)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getAsset(Long id) {
        return mapper.toResponse(requireAsset(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentHistoryResponse> assetHistory(Long id) {
        requireAsset(id);
        return assignments.findByAssetIdOrderByAssignedAtDesc(id).stream().map(mapper::toHistory).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AssetSummaryResponse summary() {
        return summarize(assets.countByTypeAndStatus());
    }

    @Override
    @Transactional
    public AssetResponse updateStatus(Long id, UpdateStatusRequest request) {
        if (request.status() == AssetStatus.ASSIGNED) throw new IllegalArgumentException("ASSIGNED is set only by assignment");
        Asset asset = requireAsset(id);
        if (asset.getStatus() == AssetStatus.ASSIGNED) {
            throw new AssetConflictException("Return the asset before changing its status");
        }
        asset.setStatus(request.status());
        asset.setUpdatedAt(LocalDateTime.now(clock));
        return mapper.toResponse(assets.save(asset));
    }

    @Override
    @Transactional
    public AssetResponse assignAsset(Long id, AssignAssetRequest request, AuthenticatedUser caller) {
        Asset asset = requireAsset(id);
        verifyExpectedType(asset, request.expectedType());
        assignAvailableAsset(asset, request, caller);
        return mapper.toResponse(requireAsset(id));
    }

    private void assignAvailableAsset(Asset asset, AssignAssetRequest request, AuthenticatedUser caller) {
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = assets.assignIfAvailable(asset.getId(), request.employeeId(), request.employeeName().trim(), now);
        if (updated == 0) throw new AssetConflictException("Asset " + asset.getAssetTag() + " is not available");
        assignments.save(new AssetAssignment(asset.getId(), request.employeeId(), request.employeeName().trim(),
                caller.fullName(), now));
    }

    @Override
    @Transactional
    public AssetResponse returnAsset(Long id, AuthenticatedUser caller) {
        Asset asset = requireAsset(id);
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = assets.releaseIfAssigned(id, now);
        if (updated == 0) throw new AssetConflictException("Asset " + asset.getAssetTag() + " is not assigned");
        closeAssignment(id, caller.fullName(), now);
        return mapper.toResponse(requireAsset(id));
    }

    private void closeAssignment(Long id, String returnedBy, LocalDateTime now) {
        AssetAssignment assignment = assignments.findFirstByAssetIdAndReturnedAtIsNullOrderByAssignedAtDesc(id)
                .orElseThrow(() -> new AssetConflictException("Open assignment history was not found"));
        assignment.close(now, returnedBy);
        assignments.save(assignment);
    }

    private Asset requireAsset(Long id) {
        return assets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
    }

    private void verifyExpectedType(Asset asset, AssetType expectedType) {
        if (asset.getType() != expectedType) {
            throw new AssetConflictException("Asset type does not match: expected " + expectedType
                    + " but the asset is " + asset.getType());
        }
    }

    private AssetSummaryResponse summarize(List<AssetCountProjection> counts) {
        Map<AssetType, Map<AssetStatus, Long>> matrix = new EnumMap<>(AssetType.class);
        for (AssetType type : AssetType.values()) matrix.put(type, zeroStatuses());
        counts.forEach(row -> matrix.get(row.getType()).put(row.getStatus(), row.getCount()));
        List<AssetSummaryResponse.TypeBreakdown> byType = new ArrayList<>();
        for (AssetType type : AssetType.values()) byType.add(toBreakdown(type, matrix.get(type)));
        return toSummary(byType);
    }

    private Map<AssetStatus, Long> zeroStatuses() {
        Map<AssetStatus, Long> values = new EnumMap<>(AssetStatus.class);
        for (AssetStatus status : AssetStatus.values()) values.put(status, 0L);
        return values;
    }

    private AssetSummaryResponse.TypeBreakdown toBreakdown(AssetType type, Map<AssetStatus, Long> values) {
        return new AssetSummaryResponse.TypeBreakdown(type, values.get(AssetStatus.AVAILABLE),
                values.get(AssetStatus.ASSIGNED), values.get(AssetStatus.IN_REPAIR), values.get(AssetStatus.RETIRED));
    }

    private AssetSummaryResponse toSummary(List<AssetSummaryResponse.TypeBreakdown> values) {
        long available = values.stream().mapToLong(AssetSummaryResponse.TypeBreakdown::available).sum();
        long assigned = values.stream().mapToLong(AssetSummaryResponse.TypeBreakdown::assigned).sum();
        long repair = values.stream().mapToLong(AssetSummaryResponse.TypeBreakdown::inRepair).sum();
        long retired = values.stream().mapToLong(AssetSummaryResponse.TypeBreakdown::retired).sum();
        return new AssetSummaryResponse(available + assigned + repair + retired, available, assigned, repair, retired, values);
    }

    private String tagFor(AssetType type, Long id) {
        return TAG_PREFIXES.get(type) + "-" + String.format(Locale.ROOT, "%04d", id);
    }
}
