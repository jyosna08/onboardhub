package com.onboardhub.employee.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onboardhub.employee.exception.AssetBadRequestException;
import com.onboardhub.employee.exception.AssetConflictException;
import com.onboardhub.employee.exception.AssetServiceUnavailableException;
import com.onboardhub.employee.exception.ResourceNotFoundException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class AssetErrorDecoderTest {
    private final AssetErrorDecoder decoder = new AssetErrorDecoder(new ObjectMapper());

    @Test
    void mapsAssetStatusesAndPreservesConflictText() {
        assertThat(decode(404, "{}" )).isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Asset not found");
        assertThat(decode(409, error("Asset LAP-0007 is not available")))
                .isInstanceOf(AssetConflictException.class).hasMessage("Asset LAP-0007 is not available");
        assertThat(decode(403, error("Access is denied"))).isInstanceOf(AccessDeniedException.class);
        assertThat(decode(400, error("Invalid assignment"))).isInstanceOf(AssetBadRequestException.class)
                .hasMessage("Invalid assignment");
        assertThat(decode(500, error("internal"))).isInstanceOf(AssetServiceUnavailableException.class);
    }

    @Test
    void malformedBodyUsesSafeFallbackMessage() {
        assertThat(decode(409, "not json" )).isInstanceOf(AssetConflictException.class)
                .hasMessage("Asset cannot be assigned");
    }

    private Exception decode(int status, String body) {
        Response response = Response.builder().status(status).reason("error")
                .request(Request.create(Request.HttpMethod.POST, "http://asset/api", Map.of(), null,
                        StandardCharsets.UTF_8, null))
                .body(body, StandardCharsets.UTF_8).build();
        return decoder.decode("AssetClient#assign", response);
    }

    private String error(String message) {
        return "{\"timestamp\":\"2026-01-01T00:00:00\",\"status\":409,\"error\":\"Conflict\","
                + "\"message\":\"" + message + "\",\"path\":\"/api/assets/7/assign\"}";
    }
}
