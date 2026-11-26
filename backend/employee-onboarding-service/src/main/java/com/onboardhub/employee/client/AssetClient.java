package com.onboardhub.employee.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "asset-inventory-service", configuration = AssetFeignConfiguration.class)
public interface AssetClient {
    @PostMapping("/api/assets/{id}/assign")
    AssetRemoteResponse assign(@PathVariable("id") Long assetId, @RequestBody AssetAssignRequest request);

    @PostMapping("/api/assets/{id}/return")
    AssetRemoteResponse returnAsset(@PathVariable("id") Long assetId);
}
