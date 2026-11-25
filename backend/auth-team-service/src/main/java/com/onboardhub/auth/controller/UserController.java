package com.onboardhub.auth.controller;

import com.onboardhub.auth.dto.CreateUserRequest;
import com.onboardhub.auth.dto.UserResponse;
import com.onboardhub.auth.enums.Role;
import com.onboardhub.auth.security.AuthenticatedUser;
import com.onboardhub.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Team users")
public class UserController {
    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a user account")
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return authService.createUser(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users, optionally by role")
    public List<UserResponse> list(@RequestParam Optional<Role> role) {
        return authService.listUsers(role);
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return authService.getCurrentUser(principal.userId());
    }
}
