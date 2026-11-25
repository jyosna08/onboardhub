package com.onboardhub.auth.security;

import com.onboardhub.auth.enums.Role;

public record AuthenticatedUser(Long userId, String email, Role role, String fullName) {
}
