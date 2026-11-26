package com.onboardhub.asset.security;

import com.onboardhub.asset.config.JwtProperties;
import com.onboardhub.asset.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public Claims parseAndValidate(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (claims.getIssuedAt() == null || claims.getExpiration() == null) {
            throw new IllegalArgumentException("Token is missing required time claims");
        }
        return claims;
    }

    public AuthenticatedUser toPrincipal(Claims claims) {
        Long userId = Long.valueOf(requireClaim(claims.getSubject(), "sub"));
        String email = requireClaim(claims.get("email", String.class), "email");
        Role role = Role.valueOf(requireClaim(claims.get("role", String.class), "role"));
        String fullName = requireClaim(claims.get("fullName", String.class), "fullName");
        return new AuthenticatedUser(userId, email, role, fullName);
    }

    private String requireClaim(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Token is missing " + name);
        return value;
    }
}
