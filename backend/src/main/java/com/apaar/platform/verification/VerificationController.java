package com.apaar.platform.verification;

import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.verification.VerificationDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Verifications", description = "APAAR format validation, authoritative verification, and public QR integrity lookups")
public class VerificationController {

    private final VerificationService verificationService;
    private final AccessScopeService accessScopeService;
    private final EnrolmentRepository enrolmentRepository;

    public VerificationController(VerificationService verificationService,
                                  AccessScopeService accessScopeService,
                                  EnrolmentRepository enrolmentRepository) {
        this.verificationService = verificationService;
        this.accessScopeService = accessScopeService;
        this.enrolmentRepository = enrolmentRepository;
    }

    @PostMapping("/apaar-verifications/format-validate")
    @PreAuthorize("hasAuthority('APAAR_VALIDATE')")
    @Operation(summary = "Validate structural format of an APAAR identifier")
    public FormatValidationResponseDto formatValidate(
            @Valid @RequestBody FormatValidationRequestDto request
    ) {
        if (request.studentId() != null) {
            assertStudentAccess(request.studentId());
        }
        return verificationService.validateFormat(request.studentId(), request.apaarId());
    }

    @PostMapping("/apaar-verifications/{studentId}/verify")
    @PreAuthorize("hasAuthority('APAAR_VERIFY')")
    @Operation(summary = "Request APAAR verification for a student identity")
    public ApaarVerificationResponseDto verifyStudent(
            @PathVariable("studentId") UUID studentId,
            @RequestBody(required = false) VerifyRequestDto request
    ) {
        assertStudentAccess(studentId);
        return verificationService.verifyStudent(studentId, request != null ? request.mode() : null);
    }

    @GetMapping("/verifications/{publicRef}")
    @Operation(summary = "Public verification lookup by reference token")
    public PublicVerificationDto getVerification(@PathVariable("publicRef") String publicRef) {
        return verificationService.resolvePublicVerification(publicRef);
    }

    @GetMapping("/verifications/public/{publicRef}")
    @Operation(summary = "Public verification lookup by reference token (aliased)")
    public PublicVerificationDto getPublicVerification(@PathVariable("publicRef") String publicRef) {
        return verificationService.resolvePublicVerification(publicRef);
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
}

