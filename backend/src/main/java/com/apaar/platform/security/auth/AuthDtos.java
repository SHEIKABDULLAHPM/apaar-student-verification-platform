package com.apaar.platform.security.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 128) String password) {
    }

    public record RefreshRequest(@NotBlank @Size(max = 512) String refreshToken) {
    }

    public record LogoutRequest(@NotBlank @Size(max = 512) String refreshToken) {
    }

    public record UserSummary(UUID id, String email, String fullName, List<String> roles,
                              UUID institutionId, String institutionName) {
    }

    public record TokenResponse(String accessToken, String refreshToken, long expiresIn, String tokenType,
                                UserSummary user) {
    }

    public record MessageResponse(String message) {
    }
}
