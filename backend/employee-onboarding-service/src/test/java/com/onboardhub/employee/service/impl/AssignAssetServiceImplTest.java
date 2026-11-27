package com.onboardhub.employee.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.onboardhub.employee.client.AssetAssignRequest;
import com.onboardhub.employee.client.AssetClient;
import com.onboardhub.employee.client.AssetRemoteResponse;
import com.onboardhub.employee.dto.TaskResponse;
import com.onboardhub.employee.enums.AssetType;
import com.onboardhub.employee.enums.Role;
import com.onboardhub.employee.exception.AssetConflictException;
import com.onboardhub.employee.exception.AssetServiceUnavailableException;
import com.onboardhub.employee.exception.ResourceNotFoundException;
import com.onboardhub.employee.security.AuthenticatedUser;
import com.onboardhub.employee.service.TaskAssignmentSnapshot;
import com.onboardhub.employee.service.TaskPersistencePort;
import feign.FeignException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssignAssetServiceImplTest {
    private AssetClient assetClient;
    private TaskPersistencePort persistence;
    private AssignAssetServiceImpl service;
    private final AuthenticatedUser caller = new AuthenticatedUser(2L, "it@example.com", Role.IT, "Test IT");
    private final TaskAssignmentSnapshot snapshot = new TaskAssignmentSnapshot(11L, 22L, "Asha Rao",
            "EQUIPMENT", "TODO", AssetType.LAPTOP);
    private final AssetRemoteResponse asset = new AssetRemoteResponse(7L, "LAP-0007", AssetType.LAPTOP,
            "Dell", "Latitude 5440", "serial", "ASSIGNED");
    private final TaskResponse task = new TaskResponse(11L, "Assign laptop", null, null, null,
            LocalDate.now(), false, AssetType.LAPTOP, 7L, "LAP-0007", "Dell Latitude 5440", null, "Test IT");

    @BeforeEach
    void setUp() {
        assetClient = mock(AssetClient.class);
        persistence = mock(TaskPersistencePort.class);
        service = new AssignAssetServiceImpl(assetClient, persistence);
        when(persistence.readAssignmentSnapshot(11L)).thenReturn(snapshot);
        when(assetClient.assign(any(), any())).thenReturn(asset);
        when(persistence.persistAssignment(11L, 7L, asset, caller)).thenReturn(task);
    }

    @Test
    void assignsRemotelyThenPersists() {
        assertThat(service.assignAsset(11L, 7L, caller)).isEqualTo(task);
        verify(assetClient).assign(7L, new AssetAssignRequest(22L, "Asha Rao", AssetType.LAPTOP));
        verify(persistence).persistAssignment(11L, 7L, asset, caller);
    }

    @Test
    void rejectsNonEquipmentAndCompletedTasksBeforeRemoteCall() {
        when(persistence.readAssignmentSnapshot(11L)).thenReturn(new TaskAssignmentSnapshot(11L, 22L,
                "Asha", "DOCUMENTATION", "TODO", null));
        assertThatThrownBy(() -> service.assignAsset(11L, 7L, caller)).isInstanceOf(AssetConflictException.class)
                .hasMessage("This task is not an equipment task");
        when(persistence.readAssignmentSnapshot(11L)).thenReturn(new TaskAssignmentSnapshot(11L, 22L,
                "Asha", "EQUIPMENT", "DONE", AssetType.LAPTOP));
        assertThatThrownBy(() -> service.assignAsset(11L, 7L, caller)).isInstanceOf(AssetConflictException.class)
                .hasMessage("This task is already completed");
        verify(assetClient, never()).assign(any(), any());
    }

    @Test
    void preservesConflictAndNotFoundErrors() {
        doThrow(new ResourceNotFoundException("Task not found")).when(persistence).readAssignmentSnapshot(11L);
        assertThatThrownBy(() -> service.assignAsset(11L, 7L, caller)).isInstanceOf(ResourceNotFoundException.class);
        doReturn(snapshot).when(persistence).readAssignmentSnapshot(11L);
        doThrow(new AssetConflictException("Asset is not available")).when(assetClient).assign(any(), any());
        assertThatThrownBy(() -> service.assignAsset(11L, 7L, caller)).isInstanceOf(AssetConflictException.class)
                .hasMessage("Asset is not available");
        verify(persistence, never()).persistAssignment(any(), any(), any(), any());
    }

    @Test
    void mapsFeignConnectionFailureToUnavailable() {
        when(assetClient.assign(any(), any())).thenThrow(FeignException.errorStatus("assign",
                feign.Response.builder().status(-1).request(feign.Request.create(feign.Request.HttpMethod.POST,
                        "http://asset", java.util.Map.of(), null, null, null)).build()));
        assertThatThrownBy(() -> service.assignAsset(11L, 7L, caller))
                .isInstanceOf(AssetServiceUnavailableException.class)
                .hasMessage("Asset service is unavailable, please try again");
    }

    @Test
    void persistenceFailureTriggersOneCompensationAndRethrowsOriginal() {
        IllegalStateException failure = new IllegalStateException("db failed");
        when(persistence.persistAssignment(11L, 7L, asset, caller)).thenThrow(failure);
        assertThatThrownBy(() -> service.assignAsset(11L, 7L, caller)).isSameAs(failure);
        verify(assetClient).returnAsset(7L);
    }

    @Test
    void compensationFailureIsSwallowed() {
        when(persistence.persistAssignment(11L, 7L, asset, caller)).thenThrow(new IllegalStateException("db failed"));
        when(assetClient.returnAsset(7L)).thenThrow(new RuntimeException("return failed"));
        assertThatThrownBy(() -> service.assignAsset(11L, 7L, caller)).isInstanceOf(IllegalStateException.class);
        verify(assetClient).returnAsset(7L);
    }

    @Test
    void concurrentCompletionConflictIsPreservedAfterCompensation() {
        AssetConflictException conflict = new AssetConflictException("This task is already completed");
        when(persistence.persistAssignment(11L, 7L, asset, caller)).thenThrow(conflict);
        assertThatThrownBy(() -> service.assignAsset(11L, 7L, caller)).isSameAs(conflict);
        verify(assetClient).returnAsset(7L);
    }
}
