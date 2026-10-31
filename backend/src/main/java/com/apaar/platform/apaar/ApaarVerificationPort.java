package com.apaar.platform.apaar;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Replaceable boundary for authorized APAAR verification. Implementations must be
 * explicit about whether they are authoritative. The demo uses a simulated adapter.
 */
public interface ApaarVerificationPort {
    String adapterName();
    boolean authoritative();

    ApaarVerificationResult verify(ApaarVerificationRequest request);

    record ApaarVerificationRequest(UUID studentId, String apaarId, String fullName, LocalDate dateOfBirth) {
    }

    record ApaarVerificationResult(String status, String reasonCode, String providerReference, boolean simulated,
                                   boolean authoritative, String message, Instant verifiedAt, Instant expiresAt,
                                   Map<String, Object> payload) {
    }
}
