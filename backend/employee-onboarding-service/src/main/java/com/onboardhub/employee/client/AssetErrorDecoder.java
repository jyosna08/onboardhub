package com.onboardhub.employee.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onboardhub.employee.exception.AssetBadRequestException;
import com.onboardhub.employee.exception.AssetConflictException;
import com.onboardhub.employee.exception.AssetServiceUnavailableException;
import com.onboardhub.employee.exception.ResourceNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.access.AccessDeniedException;

public class AssetErrorDecoder implements ErrorDecoder {
    private final ObjectMapper objectMapper;
    private final ErrorDecoder fallback = new Default();

    public AssetErrorDecoder(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    @Override
    public Exception decode(String methodKey, Response response) {
        String message = readMessage(response);
        return switch (response.status()) {
            case 400 -> new AssetBadRequestException(message);
            case 403 -> new AccessDeniedException(message);
            case 404 -> new ResourceNotFoundException("Asset not found");
            case 409 -> new AssetConflictException(message);
            case 500, 502, 503, 504 -> new AssetServiceUnavailableException();
            default -> response.status() >= 500 ? new AssetServiceUnavailableException()
                    : fallback.decode(methodKey, response);
        };
    }

    private String readMessage(Response response) {
        if (response.body() == null) return "Asset cannot be assigned";
        try (var input = response.body().asInputStream()) {
            String message = objectMapper.readTree(input).path("message").asText();
            return message.isBlank() ? "Asset cannot be assigned" : message;
        } catch (IOException | RuntimeException exception) {
            return "Asset cannot be assigned";
        }
    }
}
