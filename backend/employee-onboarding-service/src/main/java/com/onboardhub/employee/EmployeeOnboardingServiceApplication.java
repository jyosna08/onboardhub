package com.onboardhub.employee;

import com.onboardhub.employee.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(JwtProperties.class)
@EnableFeignClients
public class EmployeeOnboardingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmployeeOnboardingServiceApplication.class, args);
    }
}
