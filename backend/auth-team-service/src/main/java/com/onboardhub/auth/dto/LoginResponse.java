package com.onboardhub.auth.dto;

import com.onboardhub.auth.enums.Role;

public record LoginResponse(String token, Long userId, String fullName, Role role) {
}
