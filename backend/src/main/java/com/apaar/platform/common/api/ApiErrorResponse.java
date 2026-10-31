package com.apaar.platform.common.api;

import java.time.Instant;
import java.util.Map;

/** Stable, non-leaking error envelope used by every API and documented in docs/API_CONTRACTS.md. */
public record ApiErrorResponse(ApiError error) {
    public record ApiError(String code, String message, Map<String, Object> details, String requestId, Instant timestamp) {
    }

    public static ApiErrorResponse of(String code, String message, Map<String, Object> details, String requestId) {
        return new ApiErrorResponse(new ApiError(code, message, details == null ? Map.of() : details, requestId, Instant.now()));
    }
}
