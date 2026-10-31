package com.apaar.platform.security;

import com.apaar.platform.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "apaar-platform-test",
                "very-secure-32-byte-secret-key-for-unit-tests-12345",
                Duration.ofMinutes(15),
                Duration.ofDays(7)
        );
        jwtService = new JwtService(properties);
    }

    @Test
    @DisplayName("Issue access token and parse claims correctly")
    void issueAndParseToken() {
        UUID userId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId,
                "officer@aicte.test",
                "secret-hash",
                "AICTE Officer",
                instId,
                List.of("AICTE_OFFICER"),
                Collections.emptySet(),
                true,
                null
        );

        String token = jwtService.issueAccessToken(principal);
        assertThat(token).isNotBlank();

        Claims claims = jwtService.parseAccessToken(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email")).isEqualTo("officer@aicte.test");
        assertThat(claims.get("institutionId")).isEqualTo(instId.toString());
        assertThat(claims.get("roles", List.class)).contains("AICTE_OFFICER");
    }
}

