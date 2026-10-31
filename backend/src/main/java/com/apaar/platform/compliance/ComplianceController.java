package com.apaar.platform.compliance;

import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.compliance.ComplianceDtos.ComplianceEvaluationInputDto;
import com.apaar.platform.compliance.ComplianceDtos.ComplianceEvaluationResultDto;
import com.apaar.platform.compliance.ComplianceDtos.ComplianceViolationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/compliance")
@Tag(name = "Compliance", description = "Regulatory compliance checks, intake capacity violations, and automated rule evaluations")
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @GetMapping("/violations")
    @PreAuthorize("hasAuthority('COMPLIANCE_READ')")
    @Operation(summary = "List compliance violations with pagination and status filter")
    public PageResponse<ComplianceViolationDto> listViolations(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status
    ) {
        return complianceService.listViolations(page, size, status);
    }

    @PostMapping("/evaluations")
    @PreAuthorize("hasAuthority('COMPLIANCE_RUN')")
    @Operation(summary = "Trigger a new compliance evaluation for an institution and academic year")
    public ComplianceEvaluationResultDto evaluateCompliance(
            @Valid @RequestBody ComplianceEvaluationInputDto input
    ) {
        return complianceService.evaluateCompliance(input.institutionId(), input.academicYearId());
    }
}

