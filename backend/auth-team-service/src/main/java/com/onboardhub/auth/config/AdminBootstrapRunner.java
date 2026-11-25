package com.onboardhub.auth.config;

import com.onboardhub.auth.entity.User;
import com.onboardhub.auth.enums.Role;
import com.onboardhub.auth.repository.UserRepository;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final AdminBootstrapProperties properties;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(AdminBootstrapProperties properties, UserRepository users,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            bootstrapSafely();
        } catch (Exception exception) {
            LOGGER.warn("Bootstrap admin setup skipped because an internal operation failed", exception);
        }
    }

    private void bootstrapSafely() {
        if (isBlank(properties.email()) || isBlank(properties.password())) {
            LOGGER.info("Bootstrap admin not configured");
            return;
        }
        if (users.existsByRole(Role.ADMIN)) {
            LOGGER.info("Bootstrap admin already exists");
            return;
        }
        createAdminWhenValid();
    }

    private void createAdminWhenValid() {
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            LOGGER.warn("Bootstrap admin skipped because the configured email is invalid");
            return;
        }
        if (users.existsByEmail(email)) {
            LOGGER.warn("Bootstrap admin skipped because the configured email belongs to a non-admin: {}", email);
            return;
        }
        if (properties.password().length() < 8) {
            LOGGER.warn("Bootstrap admin skipped because the configured password is too short");
            return;
        }
        String name = isBlank(properties.name()) ? "Administrator" : properties.name().trim();
        users.save(new User(name, email, passwordEncoder.encode(properties.password()), Role.ADMIN));
        LOGGER.info("Bootstrap admin created: {}", email);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
