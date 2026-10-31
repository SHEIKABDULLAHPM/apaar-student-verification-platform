package com.apaar.platform.security.auth;

import com.apaar.platform.audit.AuditService;
import com.apaar.platform.common.config.JwtProperties;
import com.apaar.platform.common.config.LoginProperties;
import com.apaar.platform.common.exception.ApiException;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.CustomUserDetailsService;
import com.apaar.platform.security.JwtService;
import com.apaar.platform.security.RefreshToken;
import com.apaar.platform.security.RefreshTokenRepository;
import com.apaar.platform.security.User;
import com.apaar.platform.security.UserPrincipal;
import com.apaar.platform.security.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final InstitutionRepository institutionRepository;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final LoginProperties loginProperties;
    private final AuditService auditService;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String dummyHash;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       InstitutionRepository institutionRepository, CustomUserDetailsService userDetailsService,
                       PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties jwtProperties,
                       LoginProperties loginProperties, AuditService auditService, Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.institutionRepository = institutionRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.loginProperties = loginProperties;
        this.auditService = auditService;
        this.clock = clock;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        Instant now = clock.instant();
        User user = userRepository.findByEmailIgnoreCase(request.email().trim()).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyHash);
            throw new BadCredentialsException("Invalid credentials");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Account is temporarily locked after repeated failed logins");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "This account is not active");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= loginProperties.maxFailedAttempts()) {
                user.setLockedUntil(now.plus(loginProperties.lockDuration()));
                user.setFailedLoginAttempts(0);
            }
            userRepository.save(user);
            auditService.record("AUTH_LOGIN", "USER", user.getId().toString(), "LOGIN", "FAILURE",
                    user.getInstitutionId(), Map.of("reason", "BAD_CREDENTIALS"));
            throw new BadCredentialsException("Invalid credentials");
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        userRepository.save(user);

        UserPrincipal principal = userDetailsService.loadById(user.getId());
        AuthDtos.TokenResponse response = issueTokens(principal);
        auditService.record("AUTH_LOGIN", "USER", user.getId().toString(), "LOGIN", "SUCCESS",
                user.getInstitutionId(), Map.of());
        return response;
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest request) {
        Instant now = clock.instant();
        RefreshToken existing = refreshTokenRepository.findByTokenHash(sha256(request.refreshToken()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired"));
        if (!existing.isActive(now)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
        }
        UserPrincipal principal = userDetailsService.loadById(existing.getUserId());
        if (!principal.isEnabled() || !principal.isAccountNonLocked()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ACCOUNT_UNAVAILABLE", "Account is not available");
        }
        AuthDtos.TokenResponse response = issueTokens(principal);
        RefreshToken replacement = refreshTokenRepository.findByTokenHash(sha256(response.refreshToken()))
                .orElseThrow(() -> new IllegalStateException("Replacement refresh token was not persisted"));
        existing.setRevokedAt(now);
        existing.setReplacedBy(replacement.getId());
        refreshTokenRepository.save(existing);
        return response;
    }

    @Transactional
    public void logout(AuthDtos.LogoutRequest request, UserPrincipal principal) {
        refreshTokenRepository.findByTokenHash(sha256(request.refreshToken())).ifPresent(token -> {
            if (token.getUserId().equals(principal.getUserId()) && token.getRevokedAt() == null) {
                token.setRevokedAt(clock.instant());
                refreshTokenRepository.save(token);
            }
        });
        auditService.record("AUTH_LOGOUT", "USER", principal.getUserId().toString(), "LOGOUT", "SUCCESS",
                principal.getInstitutionId(), Map.of());
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserSummary me(UserPrincipal principal) {
        return summary(userDetailsService.loadById(principal.getUserId()));
    }

    private AuthDtos.TokenResponse issueTokens(UserPrincipal principal) {
        String accessToken = jwtService.issueAccessToken(principal);
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = clock.instant().plus(jwtProperties.refreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(principal.getUserId(), sha256(refreshToken), expiresAt));
        return new AuthDtos.TokenResponse(accessToken, refreshToken, jwtService.accessTokenTtlSeconds(),
                "Bearer", summary(principal));
    }

    private AuthDtos.UserSummary summary(UserPrincipal principal) {
        String institutionName = principal.getInstitutionId() == null ? null
                : institutionRepository.findById(principal.getInstitutionId()).map(i -> i.getName()).orElse(null);
        return new AuthDtos.UserSummary(principal.getUserId(), principal.getUsername(), principal.getFullName(),
                principal.getRoles(), principal.getInstitutionId(), institutionName);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
