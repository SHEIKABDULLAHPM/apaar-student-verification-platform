package com.apaar.platform.ingestion;

import com.apaar.platform.ingestion.dto.ImportDtos.*;
import com.apaar.platform.ingestion.service.SubmissionService;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/submissions/students")
@Tag(name = "Direct Student Submissions", description = "REST APIs for direct programmatic student and enrolment ingestion")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final AccessScopeService accessScopeService;

    public SubmissionController(SubmissionService submissionService, AccessScopeService accessScopeService) {
        this.submissionService = submissionService;
        this.accessScopeService = accessScopeService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STUDENT_WRITE') and hasAuthority('ENROLMENT_WRITE')")
    @Operation(summary = "Submit a single student enrolment record")
    public ResponseEntity<SingleSubmissionResult> submitSingle(@Valid @RequestBody SingleSubmissionRequest request) {
        UUID effectiveInstId = accessScopeService.requireWritableInstitution(request.institutionId());
        UserPrincipal principal = accessScopeService.requirePrincipal();

        SingleSubmissionResult result = submissionService.submitSingle(request, effectiveInstId, principal.getUserId());
        if (!result.success()) {
            return ResponseEntity.badRequest().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('STUDENT_WRITE') and hasAuthority('ENROLMENT_WRITE')")
    @Operation(summary = "Submit multiple student enrolment records in a single batch")
    public ResponseEntity<BatchSubmissionResult> submitBatch(@Valid @RequestBody BatchSubmissionRequest request) {
        UUID effectiveInstId = request.institutionId() != null
                ? accessScopeService.requireWritableInstitution(request.institutionId())
                : null;
        for (SingleSubmissionRequest record : request.records()) {
            if (record.institutionId() != null) {
                accessScopeService.requireWritableInstitution(record.institutionId());
            } else if (effectiveInstId == null) {
                // No batch-level institution was supplied, so fall back to the caller's
                // institution scope. Platform-wide accounts must specify one explicitly.
                effectiveInstId = accessScopeService.requireWritableInstitution(null);
            }
        }
        UserPrincipal principal = accessScopeService.requirePrincipal();

        BatchSubmissionResult result = submissionService.submitBatch(request, effectiveInstId, principal.getUserId());
        return ResponseEntity.ok(result);
    }
}

