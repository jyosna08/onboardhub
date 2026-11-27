package com.onboardhub.employee.service;

import com.onboardhub.employee.client.AssetRemoteResponse;
import com.onboardhub.employee.dto.TaskResponse;
import com.onboardhub.employee.security.AuthenticatedUser;

public interface TaskPersistencePort {
    TaskAssignmentSnapshot readAssignmentSnapshot(Long taskId);
    TaskResponse persistAssignment(Long taskId, Long assetId, AssetRemoteResponse asset, AuthenticatedUser caller);
}
