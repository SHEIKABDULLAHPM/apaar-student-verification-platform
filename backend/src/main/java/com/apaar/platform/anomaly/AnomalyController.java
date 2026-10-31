package com.apaar.platform.anomaly;

import com.apaar.platform.anomaly.AnomalyDtos.AnomalyDto;
import com.apaar.platform.anomaly.AnomalyDtos.AnomalyReviewInputDto;
import com.apaar.platform.common.api.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/anomalies")
@Tag(name = "AI Regulatory Intelligence", description = "AI-assisted anomaly detection, risk scoring, and triaging")
public class AnomalyController {

    private final AnomalyService anomalyService;

    public AnomalyController(AnomalyService anomalyService) {
        this.anomalyService = anomalyService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ANOMALY_READ')")
    @Operation(summary = "List flagged AI anomalies with pagination and status filter")
    public PageResponse<AnomalyDto> listAnomalies(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status
    ) {
        return anomalyService.listAnomalies(page, size, status);
    }

    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAuthority('ANOMALY_REVIEW')")
    @Operation(summary = "Review and triage an anomaly alert")
    public AnomalyDto reviewAnomaly(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AnomalyReviewInputDto input
    ) {
        return anomalyService.reviewAnomaly(id, input);
    }
}

