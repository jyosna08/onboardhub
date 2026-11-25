package com.onboardhub.auth.service;

import com.onboardhub.auth.dto.CreateUserRequest;
import com.onboardhub.auth.dto.LoginRequest;
import com.onboardhub.auth.dto.LoginResponse;
import com.onboardhub.auth.dto.RegisterRequest;
import com.onboardhub.auth.dto.UserResponse;
import com.onboardhub.auth.enums.Role;
import java.util.List;
import java.util.Optional;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    UserResponse createUser(CreateUserRequest request);
    LoginResponse login(LoginRequest request);
    List<UserResponse> listUsers(Optional<Role> role);
    UserResponse getCurrentUser(Long userId);
}
