package com.onboardhub.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.argThat;

import com.onboardhub.auth.dto.CreateUserRequest;
import com.onboardhub.auth.dto.LoginRequest;
import com.onboardhub.auth.dto.RegisterRequest;
import com.onboardhub.auth.entity.User;
import com.onboardhub.auth.enums.Role;
import com.onboardhub.auth.exception.DuplicateEmailException;
import com.onboardhub.auth.exception.ForbiddenOperationException;
import com.onboardhub.auth.exception.InvalidCredentialsException;
import com.onboardhub.auth.exception.ResourceNotFoundException;
import com.onboardhub.auth.mapper.UserMapper;
import com.onboardhub.auth.repository.UserRepository;
import com.onboardhub.auth.security.TokenService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock private UserRepository users;
    @Mock private PasswordEncoder encoder;
    @Mock private TokenService jwtService;
    private UserMapper mapper;
    @InjectMocks private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = new UserMapper();
        service = new AuthServiceImpl(users, encoder, jwtService, mapper);
    }

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        when(users.existsByEmail("asha@example.com")).thenReturn(false);
        when(encoder.encode("Password@123")).thenReturn("hashed-password");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.register(new RegisterRequest("Asha Rao", " ASHA@EXAMPLE.COM ", "Password@123", null));

        assertThat(response.email()).isEqualTo("asha@example.com");
        assertThat(response.role()).isEqualTo(Role.EMPLOYEE);
        verify(users).save(argThat(user -> user.getEmail().equals("asha@example.com")
                && user.getPasswordHash().equals("hashed-password")));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(users.existsByEmail("asha@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(new RegisterRequest("Asha", "asha@example.com", "Password@123", null)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"HR", "ADMIN", "IT"})
    void registerRejectsPrivilegedRoles(Role role) {
        assertThatThrownBy(() -> service.register(new RegisterRequest("Asha", "asha@example.com", "Password@123", role)))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only administrators can create HR, IT or admin accounts");
    }

    @Test
    void registerAcceptsEmployeeRole() {
        when(users.existsByEmail("asha@example.com")).thenReturn(false);
        when(encoder.encode("Password@123")).thenReturn("encoded");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.register(new RegisterRequest("Asha", "asha@example.com", "Password@123", Role.EMPLOYEE)).role())
                .isEqualTo(Role.EMPLOYEE);
    }

    @Test
    void loginReturnsJwtForMatchingPassword() {
        User user = user(Role.EMPLOYEE, "asha@example.com", "encoded");
        when(users.findByEmail("asha@example.com")).thenReturn(Optional.of(user));
        when(encoder.matches("Password@123", "encoded")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        assertThat(service.login(new LoginRequest("ASHA@example.com", "Password@123")).token()).isEqualTo("jwt-token");
    }

    @Test
    void unknownAndWrongPasswordUseSameMessage() {
        when(users.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        when(users.findByEmail("asha@example.com")).thenReturn(Optional.of(user(Role.EMPLOYEE, "asha@example.com", "encoded")));
        when(encoder.matches("wrong", "encoded")).thenReturn(false);
        assertThatThrownBy(() -> service.login(new LoginRequest("unknown@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class).hasMessage("Invalid email or password");
        assertThatThrownBy(() -> service.login(new LoginRequest("asha@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class).hasMessage("Invalid email or password");
    }

    @Test
    void createUserHashesPasswordAndDetectsDuplicate() {
        when(users.existsByEmail("hr@example.com")).thenReturn(false, true);
        when(encoder.encode("Password@123")).thenReturn("encoded");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new CreateUserRequest("Ravi HR", "HR@example.com", "Password@123", Role.HR);
        assertThat(service.createUser(request).role()).isEqualTo(Role.HR);
        assertThatThrownBy(() -> service.createUser(request)).isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void listsAllOrFilteredUsers() {
        User user = user(Role.HR, "hr@example.com", "encoded");
        when(users.findByRoleOrderByCreatedAtDesc(Role.HR)).thenReturn(List.of(user));
        when(users.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(user));
        assertThat(service.listUsers(Optional.of(Role.HR))).hasSize(1);
        assertThat(service.listUsers(Optional.empty())).hasSize(1);
    }

    @Test
    void getsCurrentUserAndReportsMissingUser() {
        when(users.findById(7L)).thenReturn(Optional.of(user(Role.IT, "it@example.com", "encoded")));
        when(users.findById(8L)).thenReturn(Optional.empty());
        assertThat(service.getCurrentUser(7L).email()).isEqualTo("it@example.com");
        assertThatThrownBy(() -> service.getCurrentUser(8L)).isInstanceOf(ResourceNotFoundException.class);
    }

    private User user(Role role, String email, String hash) {
        return new User("Test User", email, hash, role);
    }
}
