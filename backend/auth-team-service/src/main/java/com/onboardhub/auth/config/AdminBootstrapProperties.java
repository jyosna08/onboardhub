package com.onboardhub.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record AdminBootstrapProperties(String name, String email, String password) {
}
