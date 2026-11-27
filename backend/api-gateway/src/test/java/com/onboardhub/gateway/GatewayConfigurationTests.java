package com.onboardhub.gateway;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class GatewayConfigurationTests {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Autowired
    private Environment environment;

    @Test
    void definesAllServiceRoutesAndPaths() {
        Map<String, RouteDefinition> routes = routeDefinitionLocator.getRouteDefinitions()
                .collectMap(RouteDefinition::getId)
                .block();

        assertThat(routes).containsKeys(
                "auth-team-service", "employee-onboarding-service", "asset-inventory-service");
        assertThat(paths(routes.get("auth-team-service")))
                .containsExactlyInAnyOrder("/api/auth/**", "/api/users/**");
        assertThat(paths(routes.get("employee-onboarding-service")))
                .containsExactlyInAnyOrder("/api/employees/**", "/api/onboarding/**");
        assertThat(paths(routes.get("asset-inventory-service")))
                .containsExactly("/api/assets/**");
    }

    @Test
    void configuresLocalFrontendCorsOrigin() {
        assertThat(environment.getProperty(
                "spring.cloud.gateway.globalcors.cors-configurations.[/**].allowed-origins"))
                .contains("http://localhost:3001");
    }

    private Set<String> paths(RouteDefinition route) {
        return route.getPredicates().stream()
                .filter(predicate -> "Path".equals(predicate.getName()))
                .flatMap(predicate -> predicate.getArgs().values().stream())
                .collect(Collectors.toSet());
    }
}
