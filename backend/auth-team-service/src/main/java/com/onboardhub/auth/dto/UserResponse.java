package com.onboardhub.auth.dto;

import com.onboardhub.auth.enums.Role;

public record UserResponse(Long id, String fullName, String email, Role role) {
}
