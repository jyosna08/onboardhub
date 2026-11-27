package com.onboardhub.employee.security;

import com.onboardhub.employee.enums.Role;

public record AuthenticatedUser(Long userId, String email, Role role, String fullName) {
}
