package com.onboardhub.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.onboardhub.auth.entity.User;
import com.onboardhub.auth.enums.Role;
import com.onboardhub.auth.repository.UserRepository;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminBootstrapRunnerTest {
    private final UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void createsHashedAdminWhenConfiguredAndNoneExists() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(false);
        when(users.existsByEmail("admin@example.com")).thenReturn(false);
        AtomicReference<User> created = new AtomicReference<>();
        when(users.save(any(User.class))).thenAnswer(call -> {
            created.set(call.getArgument(0));
            return call.getArgument(0);
        });
        runner("Administrator", "ADMIN@example.com", "Password@123").run(args());
        assertThat(created.get().getRole()).isEqualTo(Role.ADMIN);
        assertThat(created.get().getEmail()).isEqualTo("admin@example.com");
        assertThat(encoder.matches("Password@123", created.get().getPasswordHash())).isTrue();
    }

    @Test
    void doesNothingWhenAdminAlreadyExists() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(true);
        runner("Admin", "admin@example.com", "Password@123").run(args());
        verify(users, never()).save(any(User.class));
    }

    @ParameterizedTest
    @CsvSource({"Admin,,Password@123", "Admin,admin@example.com,''"})
    void doesNothingWhenEmailOrPasswordIsBlank(String name, String email, String password) {
        runner(name, email, password).run(args());
        verify(users, never()).save(any(User.class));
    }

    @Test
    void skipsInvalidEmailAndShortPasswordWithoutThrowing() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(false);
        assertThatCode(() -> runner("Admin", "not-an-email", "Password@123").run(args())).doesNotThrowAnyException();
        assertThatCode(() -> runner("Admin", "admin@example.com", "short").run(args())).doesNotThrowAnyException();
        verify(users, never()).save(any(User.class));
    }

    @Test
    void skipsAnEmailOwnedByNonAdminWithoutChangingTheUser() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(false);
        when(users.existsByEmail("existing@example.com")).thenReturn(true);
        runner("Admin", "existing@example.com", "Password@123").run(args());
        verify(users, never()).save(any(User.class));
    }

    @Test
    void runningTwiceCreatesOnlyOneAdmin() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(false, true);
        when(users.existsByEmail("admin@example.com")).thenReturn(false);
        runner("Admin", "admin@example.com", "Password@123").run(args());
        runner("Admin", "admin@example.com", "Password@123").run(args());
        verify(users).save(any(User.class));
    }

    private AdminBootstrapRunner runner(String name, String email, String password) {
        return new AdminBootstrapRunner(new AdminBootstrapProperties(name, email, password), users, encoder);
    }

    private DefaultApplicationArguments args() {
        return new DefaultApplicationArguments(new String[0]);
    }
}
