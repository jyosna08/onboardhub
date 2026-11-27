package com.onboardhub.employee.service.impl;

import com.onboardhub.employee.client.AssetAssignRequest;
import com.onboardhub.employee.client.AssetClient;
import com.onboardhub.employee.client.AssetRemoteResponse;
import com.onboardhub.employee.dto.TaskResponse;
import com.onboardhub.employee.exception.AssetConflictException;
import com.onboardhub.employee.exception.AssetServiceUnavailableException;
import com.onboardhub.employee.security.AuthenticatedUser;
import com.onboardhub.employee.service.AssignAssetService;
import com.onboardhub.employee.service.TaskAssignmentSnapshot;
import com.onboardhub.employee.service.TaskPersistencePort;
import feign.FeignException;
import feign.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AssignAssetServiceImpl implements AssignAssetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AssignAssetServiceImpl.class);
    private final AssetClient assetClient;
    private final TaskPersistencePort persistence;

    public AssignAssetServiceImpl(AssetClient assetClient, TaskPersistencePort persistence) {
        this.assetClient = assetClient;
        this.persistence = persistence;
    }

    @Override
    public TaskResponse assignAsset(Long taskId, Long assetId, AuthenticatedUser caller) {
        TaskAssignmentSnapshot snapshot = persistence.readAssignmentSnapshot(taskId);
        validateTask(snapshot);
        AssetRemoteResponse asset = assignRemotely(assetId, snapshot);
        try {
            return persistence.persistAssignment(taskId, assetId, asset, caller);
        } catch (RuntimeException | Error failure) {
            compensate(assetId);
            throw failure;
        }
    }

    private void validateTask(TaskAssignmentSnapshot snapshot) {
        if (!"EQUIPMENT".equals(snapshot.category())) {
            throw new AssetConflictException("This task is not an equipment task");
        }
        if ("DONE".equals(snapshot.status())) {
            throw new AssetConflictException("This task is already completed");
        }
        if (snapshot.assetType() == null) throw new AssetConflictException("Equipment task has no asset type");
    }

    private AssetRemoteResponse assignRemotely(Long assetId, TaskAssignmentSnapshot snapshot) {
        try {
            return assetClient.assign(assetId, new AssetAssignRequest(snapshot.employeeId(),
                    snapshot.employeeName(), snapshot.assetType()));
        } catch (RetryableException exception) {
            throw new AssetServiceUnavailableException();
        } catch (FeignException exception) {
            if (exception.status() < 0 || exception.status() >= 500) throw new AssetServiceUnavailableException();
            throw exception;
        }
    }

    private void compensate(Long assetId) {
        try {
            assetClient.returnAsset(assetId);
            LOGGER.error("Asset assignment compensation attempted for assetId={}", assetId);
        } catch (RuntimeException | Error failure) {
            LOGGER.warn("Asset assignment compensation failed for assetId={}", assetId);
            LOGGER.error("Asset assignment compensation attempted for assetId={}", assetId);
        }
    }
}
