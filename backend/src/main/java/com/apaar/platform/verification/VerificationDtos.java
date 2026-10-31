package com.apaar.platform.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public class VerificationDtos {

    public record FormatValidationRequestDto(
            @NotNull UUID studentId,
            @NotBlank String apaarId
    ) {}

    public record FormatValidationResponseDto(
            boolean valid,
            String reasonCode,
            String message
    ) {}

    public record VerifyRequestDto(
            String mode
    ) {}

    public record ApaarVerificationResponseDto(
            String status,
            String mode,
            Instant verifiedAt,
            String adapterName,
            String message
    ) {}

    public record PublicVerificationDto(
            String status,
            String studentNameMasked,
            String institutionName,
            String programmeName,
            String academicYear,
            Instant verifiedAt
    ) {}
}

