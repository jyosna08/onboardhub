package com.onboardhub.employee.client;

import static org.assertj.core.api.Assertions.assertThat;

import feign.RequestTemplate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AuthorizationForwardingInterceptorTest {
    private final AssetFeignConfiguration configuration = new AssetFeignConfiguration();

    @AfterEach
    void clearRequestContext() { RequestContextHolder.resetRequestAttributes(); }

    @Test
    void forwardsAuthorizationHeaderFromCurrentRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer sample-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        RequestTemplate template = new RequestTemplate();
        configuration.authorizationForwardingInterceptor().apply(template);
        assertThat(template.headers().get("Authorization")).containsExactly("Bearer sample-token");
    }

    @Test
    void doesNotAddAuthorizationWithoutInboundHeader() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        RequestTemplate template = new RequestTemplate();
        configuration.authorizationForwardingInterceptor().apply(template);
        assertThat(template.headers()).doesNotContainKey("Authorization");
    }
}
