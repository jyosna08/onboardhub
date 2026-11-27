package com.onboardhub.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.onboardhub.auth.config.JwtProperties;
import com.onboardhub.auth.entity.User;
import com.onboardhub.auth.enums.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {
    private static final String SECRET = "test-only-secret-that-is-at-least-32-bytes";

    @Test
    void tokenClaimsRoundTrip() {
        JwtService service = new JwtService(new JwtProperties(SECRET, 60_000));
        User user = user();
        String token = service.generateToken(user);
        var claims = service.parseAndValidate(token);
        var parsed = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(token);
        assertThat(parsed.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(claims.getSubject()).isEqualTo("17");
        assertThat(claims.get("email", String.class)).isEqualTo("person@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("HR");
        assertThat(claims.get("fullName", String.class)).isEqualTo("Test Person");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void rejectsTamperedToken() {
        JwtService service = new JwtService(new JwtProperties(SECRET, 60_000));
        String token = service.generateToken(user());
        String tampered = token.substring(0, token.length() - 2) + "aa";
        assertThatThrownBy(() -> service.parseAndValidate(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        JwtService service = new JwtService(new JwtProperties(SECRET, -1));
        String token = service.generateToken(user());
        assertThatThrownBy(() -> service.parseAndValidate(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", 1000)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("32 bytes");
    }

    private User user() {
        User user = new User("Test Person", "person@example.com", "hash", Role.HR);
        ReflectionTestUtils.setField(user, "id", 17L);
        return user;
    }
}
