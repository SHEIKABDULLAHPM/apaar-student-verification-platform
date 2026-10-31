package com.apaar.platform.verification;

import com.apaar.platform.academic.AcademicYear;
import com.apaar.platform.academic.AcademicYearRepository;
import com.apaar.platform.academic.Programme;
import com.apaar.platform.academic.ProgrammeRepository;
import com.apaar.platform.apaar.*;
import com.apaar.platform.audit.AuditService;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.institution.Institution;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import com.apaar.platform.verification.VerificationDtos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class VerificationService {

    private final ApaarFormatValidator formatValidator;
    private final ApaarFormatValidationRepository formatValidationRepository;
    private final ApaarVerificationRecordRepository verificationRecordRepository;
    private final ApaarVerificationPort verificationPort;
    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("simulatedApaarVerificationAdapter")
    private ApaarVerificationPort simulatedPort;

    @Autowired(required = false)
    @org.springframework.beans.factory.annotation.Qualifier("authoritativeApaarVerificationAdapter")
    private ApaarVerificationPort authoritativePort;

    private final VerificationReferenceRepository referenceRepository;
    private final VerificationEventRepository eventRepository;
    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final InstitutionRepository institutionRepository;
    private final ProgrammeRepository programmeRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AccessScopeService accessScope;
    private final AuditService auditService;

    public VerificationService(ApaarFormatValidator formatValidator,
                               ApaarFormatValidationRepository formatValidationRepository,
                               ApaarVerificationRecordRepository verificationRecordRepository,
                               @Autowired(required = false) @org.springframework.beans.factory.annotation.Qualifier("simulatedApaarVerificationAdapter") ApaarVerificationPort verificationPort,
                               VerificationReferenceRepository referenceRepository,
                               VerificationEventRepository eventRepository,
                               StudentRepository studentRepository,
                               EnrolmentRepository enrolmentRepository,
                               InstitutionRepository institutionRepository,
                               ProgrammeRepository programmeRepository,
                               AcademicYearRepository academicYearRepository,
                               AccessScopeService accessScope,
                               AuditService auditService) {
        this.formatValidator = formatValidator;
        this.formatValidationRepository = formatValidationRepository;
        this.verificationRecordRepository = verificationRecordRepository;
        this.verificationPort = verificationPort;
        this.referenceRepository = referenceRepository;
        this.eventRepository = eventRepository;
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.institutionRepository = institutionRepository;
        this.programmeRepository = programmeRepository;
        this.academicYearRepository = academicYearRepository;
        this.accessScope = accessScope;
        this.auditService = auditService;
    }

    @Transactional
    public FormatValidationResponseDto validateFormat(UUID studentId, String apaarId) {
        ApaarFormatValidator.Result result = formatValidator.validate(apaarId);

        UUID actorId = accessScope.requirePrincipal().getUserId();
        ApaarFormatValidation record = new ApaarFormatValidation(studentId, apaarId, result.valid(), result.reasonCode(), actorId);
        formatValidationRepository.save(record);

        if (studentId != null) {
            studentRepository.findById(studentId).ifPresent(s -> {
                s.setApaarFormatValid(result.valid());
                s.setApaarFormatValidatedAt(Instant.now());
                studentRepository.save(s);
            });
        }

        return new FormatValidationResponseDto(result.valid(), result.reasonCode(), result.message());
    }

    @Transactional
    public ApaarVerificationResponseDto verifyStudent(UUID studentId, String mode) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("STUDENT_NOT_FOUND", "Student not found: " + studentId));

        if (student.getApaarId() == null || student.getApaarId().isBlank()) {
            throw new IllegalArgumentException("Student has no APAAR identifier assigned");
        }

        UUID actorId = accessScope.requirePrincipal().getUserId();
        ApaarVerificationPort.ApaarVerificationResult portResult;
        String adapterName = "simulated-apaar-adapter";

        ApaarVerificationPort targetPort = "AUTHORIZED".equalsIgnoreCase(mode)
                ? (authoritativePort != null ? authoritativePort : verificationPort)
                : (simulatedPort != null ? simulatedPort : verificationPort);

        if (targetPort != null) {
            adapterName = targetPort.adapterName();
            portResult = targetPort.verify(new ApaarVerificationPort.ApaarVerificationRequest(
                    student.getId(), student.getApaarId(), student.getFullName(), student.getDateOfBirth()
            ));
        } else {
            Instant now = Instant.now();
            portResult = new ApaarVerificationPort.ApaarVerificationResult(
                    "VERIFIED", "SIMULATED_MATCH", "SIM-" + UUID.randomUUID(), true, false,
                    "Simulated APAAR verification", now, now.plusSeconds(365L * 24 * 3600), Map.of("simulated", true)
            );
        }

        ApaarVerificationRecord record = ApaarVerificationRecord.create(
                student.getId(),
                student.getApaarId(),
                mode != null ? mode : "SIMULATED",
                adapterName,
                portResult.status(),
                portResult.providerReference(),
                portResult.reasonCode(),
                portResult.payload(),
                actorId,
                portResult.verifiedAt(),
                portResult.expiresAt()
        );
        verificationRecordRepository.save(record);

        auditService.record("APAAR_VERIFIED", "STUDENT", student.getId().toString(),
                "VERIFY", "SUCCESS", null, Map.of("adapter", adapterName, "status", portResult.status()));

        return new ApaarVerificationResponseDto(
                portResult.status(),
                record.getVerificationMode(),
                record.getVerifiedAt(),
                record.getAdapterName(),
                portResult.message()
        );
    }

    @Transactional
    public PublicVerificationDto resolvePublicVerification(String publicRef) {
        if (publicRef == null || publicRef.isBlank()) {
            throw new ResourceNotFoundException("VERIFICATION_NOT_FOUND", "Verification reference is required.");
        }

        VerificationReference ref = referenceRepository.findByPublicRef(publicRef.trim())
                .orElseThrow(() -> new ResourceNotFoundException("VERIFICATION_NOT_FOUND",
                        "Verification record not found or reference has expired/been revoked."));

        if (!"ACTIVE".equalsIgnoreCase(ref.getStatus()) || ref.getExpiresAt().isBefore(Instant.now()) || ref.getUseCount() >= ref.getMaxUses()) {
            throw new ResourceNotFoundException("VERIFICATION_NOT_FOUND",
                    "Verification record not found or reference has expired/been revoked.");
        }

        ref.setUseCount(ref.getUseCount() + 1);
        if (ref.getUseCount() >= ref.getMaxUses()) {
            ref.setStatus("EXHAUSTED");
        }
        referenceRepository.save(ref);

        VerificationEvent event = new VerificationEvent(
                ref.getId(), "PUBLIC_LOOKUP", "PUBLIC", "ANONYMOUS", "SUCCESS", Map.of("publicRef", publicRef)
        );
        eventRepository.save(event);

        Enrolment enrolment = enrolmentRepository.findById(ref.getEnrolmentId()).orElse(null);
        String studentMasked = "Student";
        String instName = "Authorized Institution";
        String progName = "Approved Programme";
        String academicYearCode = "Current Academic Year";

        if (enrolment != null) {
            if (enrolment.getStudentId() != null) {
                studentMasked = studentRepository.findById(enrolment.getStudentId())
                        .map(s -> maskName(s.getFullName()))
                        .orElse("Student");
            }
            if (enrolment.getInstitutionId() != null) {
                instName = institutionRepository.findById(enrolment.getInstitutionId())
                        .map(Institution::getName)
                        .orElse(instName);
            }
            if (enrolment.getProgrammeId() != null) {
                progName = programmeRepository.findById(enrolment.getProgrammeId())
                        .map(Programme::getName)
                        .orElse(progName);
            }
            if (enrolment.getAcademicYearId() != null) {
                academicYearCode = academicYearRepository.findById(enrolment.getAcademicYearId())
                        .map(AcademicYear::getCode)
                        .orElse(academicYearCode);
            }
        }

        return new PublicVerificationDto(
                "VERIFIED",
                studentMasked,
                instName,
                progName,
                academicYearCode,
                ref.getIssuedAt()
        );
    }

    private String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "S****";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty()) {
                sb.append(p.charAt(0)).append("****");
            }
            if (i < parts.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}

