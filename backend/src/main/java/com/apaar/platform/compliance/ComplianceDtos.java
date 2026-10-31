package com.apaar.platform.compliance;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ComplianceDtos {

    public record ComplianceViolationDto(
            String id,
            String reasonCode,
            String severity,
            String status,
            Map<String, Object> details,
            Instant createdAt
    ) {}

    public record ComplianceEvaluationInputDto(
            @NotNull UUID institutionId,
            @NotNull UUID academicYearId
    ) {}

    public record ComplianceEvaluationResultDto(
            UUID evaluationId,
            UUID institutionId,
            UUID academicYearId,
            String status,
            int totalFindings,
            Map<String, Object> summary
    ) {}
}

