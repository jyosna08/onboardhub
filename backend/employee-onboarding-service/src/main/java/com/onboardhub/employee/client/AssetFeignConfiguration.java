package com.onboardhub.employee.client;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssetFeignConfiguration {
    @Bean
    RequestInterceptor authorizationForwardingInterceptor() {
        return template -> {
            var attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
                String authorization = servlet.getRequest().getHeader("Authorization");
                if (authorization != null) template.header("Authorization", authorization);
            }
        };
    }

    @Bean
    ErrorDecoder assetErrorDecoder(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new AssetErrorDecoder(objectMapper);
    }
}
