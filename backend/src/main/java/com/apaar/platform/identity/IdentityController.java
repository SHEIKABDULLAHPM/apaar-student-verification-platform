package com.apaar.platform.identity;

import com.apaar.platform.apaar.ApaarVerificationRecord;
import com.apaar.platform.apaar.ApaarVerificationRecordRepository;
import com.apaar.platform.cases.CaseDtos.CaseDetailDto;
import com.apaar.platform.cases.CaseService;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.UserPrincipal;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import com.apaar.platform.verification.VerificationDtos.FormatValidationResponseDto;
import com.apaar.platform.verification.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/identity")
@Tag(name = "Identity Resolution", description = "APAAR validation, verification adapters, identity matching, and conflict resolution")
public class IdentityController {

    private final IdentityMatchingService matchingService;
    private final IdentityConflictService conflictService;
    private final VerificationService verificationService;
    private final ApaarVerificationRecordRepository verificationRecordRepository;
    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final CaseService caseService;
    private final AccessScopeService accessScopeService;

    public IdentityController(IdentityMatchingService matchingService,
                              IdentityConflictService conflictService,
                              VerificationService verificationService,
                              ApaarVerificationRecordRepository verificationRecordRepository,
                              StudentRepository studentRepository,
                              EnrolmentRepository enrolmentRepository,
                              CaseService caseService,
                              AccessScopeService accessScopeService) {
        this.matchingService = matchingService;
        this.conflictService = conflictService;
        this.verificationService = verificationService;
        this.verificationRecordRepository = verificationRecordRepository;
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.caseService = caseService;
        this.accessScopeService = accessScopeService;
    }

    // --- APAAR Format Validation ---
    @PostMapping("/apaar/validate")
    @PreAuthorize("hasAuthority('APAAR_VALIDATE')")
    @Operation(summary = "Validate APAAR identifier format (exactly 12 numeric digits; not authoritative verification)")
    public IdentityDtos.FormatValidationResponse validateApaar(@Valid @RequestBody IdentityDtos.ValidateApaarRequest request) {
        if (request.studentId() != null) {
            assertStudentAccess(request.studentId());
        }
        FormatValidationResponseDto res = verificationService.validateFormat(request.studentId(), request.apaarId());
        String status = res.valid() ? "FORMAT_VALID" : "FORMAT_INVALID";
        return new IdentityDtos.FormatValidationResponse(
                res.valid(),
                request.apaarId(),
                res.reasonCode(),
                res.message(),
                status,
                Instant.now()
        );
    }

    // --- APAAR Verification ---
    @PostMapping("/apaar/verification-requests")
    @PreAuthorize("hasAuthority('APAAR_VERIFY')")
    @Operation(summary = "Submit student identity for authoritative or simulated verification")
    public IdentityDtos.VerificationRecordDto requestVerification(@Valid @RequestBody IdentityDtos.VerificationRequestDto request) {
        assertStudentAccess(request.studentId());
        verificationService.verifyStudent(request.studentId(), request.mode());
        ApaarVerificationRecord record = verificationRecordRepository
                .findFirstByStudentIdOrderByRequestedAtDesc(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("VERIFICATION_RECORD_NOT_FOUND", "Verification record not found"));
        return toVerificationDto(record);
    }

    @GetMapping("/apaar/verification-requests/{verificationId}")
    @PreAuthorize("hasAuthority('APAAR_VERIFY') or hasAuthority('STUDENT_READ')")
    @Operation(summary = "Get specific APAAR verification attempt record")
    public IdentityDtos.VerificationRecordDto getVerificationRecord(@PathVariable("verificationId") UUID verificationId) {
        ApaarVerificationRecord record = verificationRecordRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("VERIFICATION_RECORD_NOT_FOUND", "Verification record not found: " + verificationId));
        assertStudentAccess(record.getStudentId());
        return toVerificationDto(record);
    }

    @GetMapping("/students/{studentId}/verification-history")
    @PreAuthorize("hasAuthority('STUDENT_READ')")
    @Operation(summary = "Get all verification history records for a student")
    public List<IdentityDtos.VerificationRecordDto> getStudentVerificationHistory(@PathVariable("studentId") UUID studentId) {
        assertStudentAccess(studentId);
        return verificationRecordRepository.findByStudentIdOrderByRequestedAtDesc(studentId).stream()
                .map(this::toVerificationDto)
                .toList();
    }

    // --- Identity Matching Runs ---
    @PostMapping("/matching-runs")
    @PreAuthorize("hasAuthority('CONFLICT_INVESTIGATE') or hasAuthority('COMPLIANCE_RUN') or hasRole('ROLE_PLATFORM_ADMIN') or hasRole('ROLE_AICTE_ADMIN')")
    @Operation(summary = "Trigger deterministic identity matching run across student records")
    public IdentityDtos.MatchingRunDto executeMatchingRun(@RequestBody IdentityDtos.MatchingRunRequest request) {
        UserPrincipal principal = accessScopeService.requirePrincipal();
        return matchingService.executeMatchingRun(request, principal);
    }

    @GetMapping("/matching-runs")
    @PreAuthorize("hasAuthority('CONFLICT_READ')")
    @Operation(summary = "List identity matching runs with pagination")
    public PageResponse<IdentityDtos.MatchingRunDto> listMatchingRuns(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return matchingService.listMatchingRuns(PageRequest.of(page, size));
    }

    @GetMapping("/matching-runs/{runId}")
    @PreAuthorize("hasAuthority('CONFLICT_READ')")
    @Operation(summary = "Get details of a specific matching run")
    public IdentityDtos.MatchingRunDto getMatchingRun(@PathVariable("runId") UUID runId) {
        return matchingService.getMatchingRun(runId);
    }

    // --- Identity Matches ---
    @GetMapping("/matches")
    @PreAuthorize("hasAuthority('CONFLICT_READ')")
    @Operation(summary = "List detected identity matches with optional classification filter")
    public PageResponse<IdentityDtos.IdentityMatchDto> listMatches(
            @RequestParam(value = "classification", required = false) String classification,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return matchingService.listMatches(classification, PageRequest.of(page, size));
    }

    @GetMapping("/matches/{matchId}")
    @PreAuthorize("hasAuthority('CONFLICT_READ')")
    @Operation(summary = "Get identity match details by ID")
    public IdentityDtos.IdentityMatchDto getMatch(@PathVariable("matchId") UUID matchId) {
        return matchingService.getMatch(matchId);
    }

    @GetMapping("/students/{studentId}/matches")
    @PreAuthorize("hasAuthority('CONFLICT_READ') or hasAuthority('STUDENT_READ')")
    @Operation(summary = "Get matches involving a specific student")
    public List<IdentityDtos.IdentityMatchDto> getMatchesForStudent(@PathVariable("studentId") UUID studentId) {
        return matchingService.getMatchesForStudent(studentId);
    }

    // --- Identity Conflicts ---
    @GetMapping("/conflicts")
    @PreAuthorize("hasAuthority('CONFLICT_READ')")
    @Operation(summary = "List identity conflicts with status filtering and pagination")
    public PageResponse<IdentityDtos.ConflictDto> listConflicts(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return conflictService.list(status, page, size);
    }

    @GetMapping("/conflicts/{conflictId}")
    @PreAuthorize("hasAuthority('CONFLICT_READ')")
    @Operation(summary = "Get identity conflict by ID")
    public IdentityDtos.ConflictDto getConflict(@PathVariable("conflictId") UUID conflictId) {
        return conflictService.get(conflictId);
    }

    @PatchMapping("/conflicts/{conflictId}")
    @PreAuthorize("hasAuthority('CONFLICT_INVESTIGATE')")
    @Operation(summary = "Update conflict review status and resolution notes")
    public IdentityDtos.ConflictDto updateConflictStatus(
            @PathVariable("conflictId") UUID conflictId,
            @Valid @RequestBody IdentityDtos.UpdateConflictRequest request) {
        UserPrincipal principal = accessScopeService.requirePrincipal();
        return conflictService.updateStatus(conflictId, request, principal);
    }

    @GetMapping("/conflicts/{conflictId}/evidence")
    @PreAuthorize("hasAuthority('CONFLICT_READ')")
    @Operation(summary = "Get comprehensive side-by-side evidence dossier for conflict")
    public IdentityDtos.ConflictEvidenceDto getConflictEvidence(@PathVariable("conflictId") UUID conflictId) {
        // Reuse conflict lookup so institution-scoped users cannot read another institution's dossier.
        conflictService.get(conflictId);
        return matchingService.getConflictEvidence(conflictId);
    }

    @PostMapping("/conflicts/{conflictId}/investigations")
    @PreAuthorize("hasAuthority('CASE_WRITE') or hasAuthority('CONFLICT_INVESTIGATE')")
    @Operation(summary = "Promote identity conflict to formal investigation case dossier")
    public CaseDetailDto createInvestigationCase(@PathVariable("conflictId") UUID conflictId) {
        UserPrincipal principal = accessScopeService.requirePrincipal();
        return caseService.createCaseFromConflict(conflictId, principal);
    }

    // --- Dashboard Metrics ---
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('DASHBOARD_READ') or hasAuthority('CONFLICT_READ')")
    @Operation(summary = "Get identity resolution dashboard metrics and counters")
    public IdentityDtos.IdentityDashboardDto getDashboardMetrics() {
        return matchingService.getDashboardMetrics();
    }

    private void assertStudentAccess(UUID studentId) {
        if (studentId == null || accessScopeService.isPlatformWide()) {
            return;
        }
        UUID ownInstitutionId = accessScopeService.requirePrincipal().getInstitutionId();
        if (ownInstitutionId == null
                || !enrolmentRepository.existsByStudentIdAndInstitutionId(studentId, ownInstitutionId)) {
            throw new ResourceNotFoundException("STUDENT_NOT_FOUND", "Student not found: " + studentId);
        }
    }

    private IdentityDtos.VerificationRecordDto toVerificationDto(ApaarVerificationRecord r) {
        String studentName = studentRepository.findById(r.getStudentId())
                .map(Student::getFullName)
                .orElse("Student");
        return new IdentityDtos.VerificationRecordDto(
                r.getId(),
                r.getStudentId(),
                studentName,
                r.getApaarId(),
                r.getStatus(),
                r.getVerificationMode(),
                r.getAdapterName(),
                r.getProviderReference(),
                r.getReasonCode(),
                r.isSimulated(),
                !r.isSimulated(),
                "Verification outcome: " + r.getStatus(),
                r.getVerifiedAt(),
                r.getRequestedAt()
        );
    }
}

