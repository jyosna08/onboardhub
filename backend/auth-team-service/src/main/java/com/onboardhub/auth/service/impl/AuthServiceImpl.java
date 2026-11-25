package com.onboardhub.auth.service.impl;

import com.onboardhub.auth.dto.CreateUserRequest;
import com.onboardhub.auth.dto.LoginRequest;
import com.onboardhub.auth.dto.LoginResponse;
import com.onboardhub.auth.dto.RegisterRequest;
import com.onboardhub.auth.dto.UserResponse;
import com.onboardhub.auth.entity.User;
import com.onboardhub.auth.enums.Role;
import com.onboardhub.auth.exception.DuplicateEmailException;
import com.onboardhub.auth.exception.ForbiddenOperationException;
import com.onboardhub.auth.exception.InvalidCredentialsException;
import com.onboardhub.auth.exception.ResourceNotFoundException;
import com.onboardhub.auth.mapper.UserMapper;
import com.onboardhub.auth.repository.UserRepository;
import com.onboardhub.auth.security.TokenService;
import com.onboardhub.auth.service.AuthService;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {
    private static final String REGISTER_ROLE_ERROR =
            "Only administrators can create HR, IT or admin accounts";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService jwtService;
    private final UserMapper userMapper;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
            TokenService jwtService, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if (request.role() != null && request.role() != Role.EMPLOYEE) {
            throw new ForbiddenOperationException(REGISTER_ROLE_ERROR);
        }
        return userMapper.toResponse(create(request.fullName(), request.email(), request.password(), Role.EMPLOYEE));
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        return userMapper.toResponse(create(request.fullName(), request.email(), request.password(), request.role()));
    }

    private User create(String fullName, String email, String password, Role role) {
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException("Email already exists");
        }
        User user = new User(fullName.trim(), normalizedEmail, passwordEncoder.encode(password), role);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return new LoginResponse(jwtService.generateToken(user), user.getId(), user.getFullName(), user.getRole());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(Optional<Role> role) {
        List<User> users = role.map(userRepository::findByRoleOrderByCreatedAtDesc)
                .orElseGet(userRepository::findAllByOrderByCreatedAtDesc);
        return users.stream().map(userMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }
}
