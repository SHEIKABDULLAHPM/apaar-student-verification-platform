package com.apaar.platform.cases;

import com.apaar.platform.audit.AuditService;
import com.apaar.platform.cases.CaseDtos.*;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.identity.IdentityConflict;
import com.apaar.platform.identity.IdentityConflictRepository;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.User;
import com.apaar.platform.security.UserPrincipal;
import com.apaar.platform.security.UserRepository;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import com.apaar.platform.student.StudentRecordVersion;
import com.apaar.platform.student.StudentRecordVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock private InvestigationCaseRepository caseRepository;
    @Mock private CaseCommentRepository commentRepository;
    @Mock private CaseEvidenceRepository evidenceRepository;
    @Mock private CaseDecisionRepository decisionRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AccessScopeService accessScope;
    @Mock private AuditService auditService;
    @Mock private IdentityConflictRepository conflictRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private EnrolmentRepository enrolmentRepository;
    @Mock private StudentRecordVersionRepository studentRecordVersionRepository;

    private CaseService caseService;

    @BeforeEach
    void setUp() {
        caseService = new CaseService(
                caseRepository,
                commentRepository,
                evidenceRepository,
                decisionRepository,
                institutionRepository,
                userRepository,
                accessScope,
                auditService,
                conflictRepository,
                studentRepository,
                enrolmentRepository,
                studentRecordVersionRepository
        );
        lenient().when(accessScope.isPlatformWide()).thenReturn(true);
    }

    @Test
    @DisplayName("Create case from conflict links evidence and updates conflict status to INVESTIGATING")
    void createCaseFromConflict() {
        UUID conflictId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        UUID enrId = UUID.randomUUID();

        IdentityConflict conflict = new IdentityConflict();
        ReflectionTestUtils.setField(conflict, "id", conflictId);
        conflict.setConflictType("CROSS_INSTITUTION_ACTIVE_ENROLMENT");
        conflict.setReasonCode("DUPLICATE_ACTIVE_ENROLMENT");
        conflict.setSeverity("HIGH");
        conflict.setStatus("OPEN");
        conflict.setPrimaryStudentId(studentId);
        conflict.setPrimaryEnrolmentId(enrId);

        Enrolment enrolment = new Enrolment();
        ReflectionTestUtils.setField(enrolment, "id", enrId);
        enrolment.setInstitutionId(instId);

        when(conflictRepository.findById(conflictId)).thenReturn(Optional.of(conflict));
        when(enrolmentRepository.findById(enrId)).thenReturn(Optional.of(enrolment));

        when(caseRepository.save(any(InvestigationCase.class))).thenAnswer(i -> {
            InvestigationCase c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });
        when(caseRepository.findById(any())).thenAnswer(i -> {
            InvestigationCase c = new InvestigationCase();
            ReflectionTestUtils.setField(c, "id", i.getArgument(0));
            c.setCaseNumber("CASE-12345678");
            c.setTitle("Identity Conflict Review");
            c.setCaseType("CROSS_INSTITUTION_ACTIVE_ENROLMENT");
            c.setStatus("OPEN");
            c.setPriority("HIGH");
            return Optional.of(c);
        });

        UserPrincipal actor = new UserPrincipal(
                UUID.randomUUID(), "officer@aicte.gov.in", "hash", "Review Officer", null,
                List.of("ROLE_AICTE_ADMIN"), List.of("CASE_READ", "CASE_WRITE", "CASE_DECIDE"), true, null
        );
        CaseDetailDto result = caseService.createCaseFromConflict(conflictId, actor);

        assertThat(result).isNotNull();
        assertThat(conflict.getStatus()).isEqualTo("INVESTIGATING");
        verify(evidenceRepository, times(1)).save(argThat(ev ->
                ev.getStorageRef().contains("conflict:" + conflictId)
        ));
        verify(conflictRepository, times(1)).save(conflict);
    }

    @Test
    @DisplayName("Recording resolution decision marks case RESOLVED, updates conflict, and versions corrected student")
    void recordResolutionDecision() {
        UUID caseId = UUID.randomUUID();
        UUID conflictId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        InvestigationCase c = new InvestigationCase();
        ReflectionTestUtils.setField(c, "id", caseId);
        c.setCaseNumber("CASE-999");
        c.setStatus("OPEN");

        CaseEvidence ev = new CaseEvidence();
        ev.setCaseId(caseId);
        ev.setStorageRef("conflict:" + conflictId);

        IdentityConflict conflict = new IdentityConflict();
        ReflectionTestUtils.setField(conflict, "id", conflictId);
        conflict.setStatus("INVESTIGATING");

        Student student = new Student();
        ReflectionTestUtils.setField(student, "id", studentId);
        student.setFullName("Incorrect Name");
        student.setDateOfBirth(LocalDate.of(2004, 1, 1));
        student.setGender("MALE");

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(c));
        when(evidenceRepository.findByCaseIdOrderByCollectedAtDesc(caseId)).thenReturn(List.of(ev));
        when(conflictRepository.findById(conflictId)).thenReturn(Optional.of(conflict));
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(studentRecordVersionRepository.findLatestVersionNo(studentId)).thenReturn(1);

        RecordDecisionRequest req = new RecordDecisionRequest(
                "DATA_ENTRY_ERROR",
                "Typographical error in student name verified by identity document",
                "CORRECT_RECORD",
                studentId,
                "Corrected Full Name",
                null,
                null
        );

        UserPrincipal actor = new UserPrincipal(
                UUID.randomUUID(), "officer@aicte.gov.in", "hash", "Review Officer", null,
                List.of("ROLE_AICTE_ADMIN"), List.of("CASE_READ", "CASE_WRITE", "CASE_DECIDE"), true, null
        );
        CaseDetailDto result = caseService.recordDecision(caseId, req, actor);

        assertThat(c.getStatus()).isEqualTo("RESOLVED");
        assertThat(conflict.getStatus()).isEqualTo("RESOLVED");
        assertThat(student.getFullName()).isEqualTo("Corrected Full Name");

        // Verify version history created
        verify(studentRecordVersionRepository, times(1)).save(argThat(v ->
                v.getStudentId().equals(studentId) && v.getVersionNo() == 2
        ));
    }

    @Test
    @DisplayName("Updating a case with an unsupported status is rejected before persistence")
    void rejectUnsupportedCaseStatus() {
        UUID caseId = UUID.randomUUID();
        InvestigationCase c = new InvestigationCase();
        ReflectionTestUtils.setField(c, "id", caseId);
        c.setCaseNumber("CASE-STATUS");
        c.setStatus("OPEN");

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(c));

        UserPrincipal actor = new UserPrincipal(
                UUID.randomUUID(), "officer@aicte.gov.in", "hash", "Review Officer", null,
                List.of("ROLE_AICTE_ADMIN"), List.of("CASE_WRITE"), true, null
        );

        assertThatThrownBy(() -> caseService.updateStatus(caseId,
                new UpdateCaseStatusRequest("NOT_A_REAL_STATUS", null), actor))
                .isInstanceOf(com.apaar.platform.common.exception.UnprocessableEntityException.class)
                .hasMessageContaining("NOT_A_REAL_STATUS");

        verify(caseRepository, never()).save(any(InvestigationCase.class));
    }
}
