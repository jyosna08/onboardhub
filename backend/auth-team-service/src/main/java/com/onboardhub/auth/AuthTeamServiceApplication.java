package com.onboardhub.auth;

import com.onboardhub.auth.config.AdminBootstrapProperties;
import com.onboardhub.auth.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({JwtProperties.class, AdminBootstrapProperties.class})
public class AuthTeamServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthTeamServiceApplication.class, args);
    }
}
