package com.onboardhub.asset.security;

import com.onboardhub.asset.enums.Role;

public record AuthenticatedUser(Long userId, String email, Role role, String fullName) {
}
