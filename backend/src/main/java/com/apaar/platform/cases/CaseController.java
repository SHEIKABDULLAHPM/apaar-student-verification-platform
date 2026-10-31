package com.apaar.platform.cases;

import com.apaar.platform.cases.CaseDtos.*;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cases")
@Tag(name = "Investigation & Cases", description = "Formal review workflows, case dossiers, and resolution tracking")
public class CaseController {

    private final CaseService caseService;
    private final AccessScopeService accessScopeService;

    public CaseController(CaseService caseService, AccessScopeService accessScopeService) {
        this.caseService = caseService;
        this.accessScopeService = accessScopeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CASE_READ')")
    @Operation(summary = "List investigation cases with status filtering and pagination")
    public PageResponse<CaseSummaryDto> listCases(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "status", required = false) String status
    ) {
        return caseService.listCases(page, size, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CASE_READ')")
    @Operation(summary = "Get full case dossier by ID including evidence, comments, and decisions")
    public CaseDetailDto getCase(@PathVariable("id") UUID id) {
        return caseService.getCase(id);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAuthority('CASE_WRITE')")
    @Operation(summary = "Add comment to investigation case dossier")
    public CaseCommentDto addComment(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AddCommentRequest request
    ) {
        UserPrincipal principal = accessScopeService.requirePrincipal();
        return caseService.addComment(id, request, principal);
    }

    @PostMapping("/{id}/decisions")
    @PreAuthorize("hasAuthority('CASE_DECIDE')")
    @Operation(summary = "Record authorized decision and formal resolution for investigation case")
    public CaseDetailDto recordDecision(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RecordDecisionRequest request
    ) {
        UserPrincipal principal = accessScopeService.requirePrincipal();
        return caseService.recordDecision(id, request, principal);
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CASE_WRITE')")
    @Operation(summary = "Update investigation case status")
    public CaseDetailDto updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateCaseStatusRequest request
    ) {
        UserPrincipal principal = accessScopeService.requirePrincipal();
        return caseService.updateStatus(id, request, principal);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAuthority('CASE_WRITE')")
    @Operation(summary = "Assign investigation case to officer")
    public CaseDetailDto assignCase(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AssignCaseRequest request
    ) {
        UserPrincipal principal = accessScopeService.requirePrincipal();
        return caseService.assignCase(id, request, principal);
    }

    @PostMapping("/{id}/evidence")
    @PreAuthorize("hasAuthority('CASE_WRITE')")
    @Operation(summary = "Add evidence artifact or verification link to case")
    public CaseEvidenceDto addEvidence(
            @PathVariable("id") UUID id,
            @Valid @RequestBody AddEvidenceRequest request
    ) {
        UserPrincipal principal = accessScopeService.requirePrincipal();
        return caseService.addEvidence(id, request, principal);
    }
}
