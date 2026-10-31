package com.apaar.platform.anomaly;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class AnomalyDtos {

    public record AnomalyDto(
            String id,
            String type,
            String anomalyType,
            String severity,
            String status,
            String description,
            String institutionName,
            String studentId,
            Instant createdAt
    ) {}

    public record AnomalyReviewInputDto(
            @NotBlank String decision,
            String notes
    ) {}
}

