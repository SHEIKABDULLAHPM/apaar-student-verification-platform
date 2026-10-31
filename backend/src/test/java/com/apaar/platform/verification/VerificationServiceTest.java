package com.apaar.platform.verification;

import com.apaar.platform.academic.AcademicYear;
import com.apaar.platform.academic.AcademicYearRepository;
import com.apaar.platform.academic.Programme;
import com.apaar.platform.academic.ProgrammeRepository;
import com.apaar.platform.apaar.*;
import com.apaar.platform.audit.AuditService;
import com.apaar.platform.institution.Institution;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.UserPrincipal;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import com.apaar.platform.verification.VerificationDtos.FormatValidationResponseDto;
import com.apaar.platform.verification.VerificationDtos.PublicVerificationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock private ApaarFormatValidator formatValidator;
    @Mock private ApaarFormatValidationRepository formatValidationRepository;
    @Mock private ApaarVerificationRecordRepository verificationRecordRepository;
    @Mock private ApaarVerificationPort verificationPort;
    @Mock private VerificationReferenceRepository referenceRepository;
    @Mock private VerificationEventRepository eventRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private EnrolmentRepository enrolmentRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private ProgrammeRepository programmeRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private AccessScopeService accessScope;
    @Mock private AuditService auditService;

    @InjectMocks
    private VerificationService verificationService;

    @Test
    @DisplayName("validateFormat calls validator and saves result")
    void validateFormatSavesRecord() {
        UUID studentId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "officer@test.local", "hash", "Officer", null,
                List.of("VERIFICATION_OFFICER"), Collections.emptySet(), true, null
        );
        when(accessScope.requirePrincipal()).thenReturn(principal);
        when(formatValidator.validate("123456789012"))
                .thenReturn(new ApaarFormatValidator.Result(true, "FORMAT_VALID", "OK"));

        FormatValidationResponseDto response = verificationService.validateFormat(studentId, "123456789012");

        assertThat(response.valid()).isTrue();
        assertThat(response.reasonCode()).isEqualTo("FORMAT_VALID");
        verify(formatValidationRepository).save(any(ApaarFormatValidation.class));
    }

    @Test
    @DisplayName("resolvePublicVerification returns masked student and programme info")
    void resolvePublicVerificationReturnsMasked() {
        String publicRef = "demo-ref-123";
        UUID enrolmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        UUID progId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();

        VerificationReference ref = new VerificationReference();
        ReflectionTestUtils.setField(ref, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(ref, "publicRef", publicRef);
        ReflectionTestUtils.setField(ref, "enrolmentId", enrolmentId);
        ReflectionTestUtils.setField(ref, "status", "ACTIVE");
        ReflectionTestUtils.setField(ref, "expiresAt", Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(ref, "maxUses", 10);
        ReflectionTestUtils.setField(ref, "useCount", 0);
        ReflectionTestUtils.setField(ref, "issuedAt", Instant.now());

        when(referenceRepository.findByPublicRef(publicRef)).thenReturn(Optional.of(ref));

        Enrolment enrolment = new Enrolment();
        ReflectionTestUtils.setField(enrolment, "studentId", studentId);
        ReflectionTestUtils.setField(enrolment, "institutionId", instId);
        ReflectionTestUtils.setField(enrolment, "programmeId", progId);
        ReflectionTestUtils.setField(enrolment, "academicYearId", yearId);

        when(enrolmentRepository.findById(enrolmentId)).thenReturn(Optional.of(enrolment));

        Student student = new Student();
        student.setFullName("Aditi Sharma");
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));

        Institution inst = new Institution();
        ReflectionTestUtils.setField(inst, "name", "Demo Institute of Technology");
        when(institutionRepository.findById(instId)).thenReturn(Optional.of(inst));

        Programme prog = new Programme();
        ReflectionTestUtils.setField(prog, "name", "B.Tech Computer Science");
        when(programmeRepository.findById(progId)).thenReturn(Optional.of(prog));

        AcademicYear ay = new AcademicYear();
        ReflectionTestUtils.setField(ay, "code", "2026-27");
        when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(ay));

        PublicVerificationDto dto = verificationService.resolvePublicVerification(publicRef);

        assertThat(dto.status()).isEqualTo("VERIFIED");
        assertThat(dto.studentNameMasked()).isEqualTo("A**** S****");
        assertThat(dto.institutionName()).isEqualTo("Demo Institute of Technology");
        assertThat(dto.programmeName()).isEqualTo("B.Tech Computer Science");
        assertThat(dto.academicYear()).isEqualTo("2026-27");
    }
}

