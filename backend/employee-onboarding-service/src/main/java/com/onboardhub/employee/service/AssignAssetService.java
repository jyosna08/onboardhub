package com.onboardhub.employee.service;

import com.onboardhub.employee.dto.TaskResponse;
import com.onboardhub.employee.security.AuthenticatedUser;

public interface AssignAssetService {
    TaskResponse assignAsset(Long taskId, Long assetId, AuthenticatedUser caller);
}
