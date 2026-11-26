package com.onboardhub.asset.controller;

import com.onboardhub.asset.dto.AssignAssetRequest;
import com.onboardhub.asset.dto.AssetResponse;
import com.onboardhub.asset.dto.AssetSummaryResponse;
import com.onboardhub.asset.dto.AssignmentHistoryResponse;
import com.onboardhub.asset.dto.CreateAssetRequest;
import com.onboardhub.asset.dto.UpdateStatusRequest;
import com.onboardhub.asset.enums.AssetStatus;
import com.onboardhub.asset.enums.AssetType;
import com.onboardhub.asset.security.AuthenticatedUser;
import com.onboardhub.asset.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets")
@Tag(name = "Assets")
public class AssetController {
    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('IT', 'ADMIN')")
    @Operation(summary = "Add an asset to inventory")
    public AssetResponse create(@Valid @RequestBody CreateAssetRequest request) {
        return assetService.createAsset(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'IT', 'ADMIN')")
    @Operation(summary = "List and filter assets")
    public List<AssetResponse> list(@RequestParam(required = false) AssetType type,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) String search) {
        return assetService.listAssets(type, status, search);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('HR', 'IT', 'ADMIN')")
    @Operation(summary = "Summarize inventory by type and status")
    public AssetSummaryResponse summary() {
        return assetService.summary();
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('HR', 'IT', 'ADMIN')")
    @Operation(summary = "Get an asset")
    public AssetResponse get(@PathVariable Long id) {
        return assetService.getAsset(id);
    }

    @GetMapping("/{id:\\d+}/history")
    @PreAuthorize("hasAnyRole('HR', 'IT', 'ADMIN')")
    @Operation(summary = "Get an asset assignment history")
    public List<AssignmentHistoryResponse> history(@PathVariable Long id) {
        return assetService.assetHistory(id);
    }

    @PatchMapping("/{id:\\d+}/status")
    @PreAuthorize("hasAnyRole('IT', 'ADMIN')")
    @Operation(summary = "Update asset status")
    public AssetResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return assetService.updateStatus(id, request);
    }

    @PostMapping("/{id:\\d+}/assign")
    @PreAuthorize("hasAnyRole('IT', 'ADMIN')")
    @Operation(summary = "Assign an available asset")
    public AssetResponse assign(@PathVariable Long id, @Valid @RequestBody AssignAssetRequest request,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return assetService.assignAsset(id, request, caller);
    }

    @PostMapping("/{id:\\d+}/return")
    @PreAuthorize("hasAnyRole('IT', 'ADMIN')")
    @Operation(summary = "Return an assigned asset")
    public AssetResponse returnAsset(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser caller) {
        return assetService.returnAsset(id, caller);
    }
}
