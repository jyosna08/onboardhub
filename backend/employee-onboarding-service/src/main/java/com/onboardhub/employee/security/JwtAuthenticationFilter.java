package com.onboardhub.employee.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final JsonSecurityErrorHandler errors;

    public JwtAuthenticationFilter(JwtService jwtService, JsonSecurityErrorHandler errors) {
        this.jwtService = jwtService;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        authenticateOrReject(request, response, chain, authorization.substring(7));
    }

    private void authenticateOrReject(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain, String token) throws ServletException, IOException {
        AuthenticatedUser principal;
        try {
            principal = jwtService.toPrincipal(jwtService.parseAndValidate(token));
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            errors.commence(request, response, new BadCredentialsException("Invalid token", exception));
            return;
        }
        var authority = new SimpleGrantedAuthority("ROLE_" + principal.role().name());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, token, List.of(authority)));
        chain.doFilter(request, response);
    }
}
