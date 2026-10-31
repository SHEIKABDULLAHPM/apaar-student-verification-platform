package com.apaar.platform.identity;

import com.apaar.platform.academic.AcademicYearRepository;
import com.apaar.platform.academic.ProgrammeRepository;
import com.apaar.platform.apaar.ApaarVerificationRecordRepository;
import com.apaar.platform.audit.AuditService;
import com.apaar.platform.cases.InvestigationCaseRepository;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.UserPrincipal;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdentityMatchingServiceTest {

    @Mock private StudentRepository studentRepository;
    @Mock private EnrolmentRepository enrolmentRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private ProgrammeRepository programmeRepository;
    @Mock private IdentityConflictRepository conflictRepository;
    @Mock private IdentityMatchingRunRepository runRepository;
    @Mock private IdentityMatchRepository matchRepository;
    @Mock private ApaarVerificationRecordRepository verificationRecordRepository;
    @Mock private InvestigationCaseRepository caseRepository;
    @Mock private AccessScopeService accessScope;
    @Mock private AuditService auditService;

    private Clock clock;
    private IdentityMatchingService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC);
        service = new IdentityMatchingService(
                studentRepository,
                enrolmentRepository,
                institutionRepository,
                academicYearRepository,
                programmeRepository,
                conflictRepository,
                runRepository,
                matchRepository,
                verificationRecordRepository,
                caseRepository,
                accessScope,
                auditService,
                clock
        );
    }

    @Test
    @DisplayName("Cross-institution concurrent active enrolment generates conflict and POTENTIAL_DUPLICATE_ENROLMENT match")
    void testCrossInstitutionConcurrentActiveEnrolment() {
        UUID instA = UUID.randomUUID();
        UUID instB = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        String apaar = "900000000001";

        Student s1 = createStudent("Student Alpha", apaar, LocalDate.of(2004, 3, 15));

        Enrolment e1 = createEnrolment(s1.getId(), instA, yearId, "ACTIVE");
        Enrolment e2 = createEnrolment(s1.getId(), instB, yearId, "ACTIVE");

        when(studentRepository.findAll()).thenReturn(List.of(s1));
        when(enrolmentRepository.findAll()).thenReturn(List.of(e1, e2));

        when(runRepository.save(any(IdentityMatchingRun.class))).thenAnswer(i -> {
            IdentityMatchingRun run = i.getArgument(0);
            ReflectionTestUtils.setField(run, "id", UUID.randomUUID());
            return run;
        });
        when(conflictRepository.findFirstByFingerprintAndStatusIn(anyString(), any())).thenReturn(Optional.empty());
        when(conflictRepository.save(any(IdentityConflict.class))).thenAnswer(i -> {
            IdentityConflict c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });

        UserPrincipal actor = new UserPrincipal(
                UUID.randomUUID(), "admin@aicte.gov.in", "hash", "AICTE Officer", null,
                List.of("ROLE_AICTE_ADMIN"), List.of("CONFLICT_INVESTIGATE"), true, null
        );
        IdentityDtos.MatchingRunRequest request = new IdentityDtos.MatchingRunRequest(null, null, "EXACT_MATCH");

        IdentityDtos.MatchingRunDto runDto = service.executeMatchingRun(request, actor);

        assertThat(runDto.matchesFound()).isGreaterThanOrEqualTo(1);
        assertThat(runDto.conflictsGenerated()).isGreaterThanOrEqualTo(1);
        verify(conflictRepository, times(1)).save(any(IdentityConflict.class));
        verify(matchRepository, atLeastOnce()).saveAll(argThat(matches -> {
            for (IdentityMatch m : matches) {
                if ("POTENTIAL_DUPLICATE_ENROLMENT".equals(m.getMatchClassification()) && m.isReviewRequired()) {
                    return true;
                }
            }
            return false;
        }));
    }

    @Test
    @DisplayName("Institution-filtered run still detects cross-institution enrolment for its students")
    void testInstitutionFilterIncludesCounterpartEnrolments() {
        UUID instA = UUID.randomUUID();
        UUID instB = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        String apaar = "900000000005";

        Student s1 = createStudent("Student Epsilon", apaar, LocalDate.of(2004, 9, 3));

        Enrolment e1 = createEnrolment(s1.getId(), instA, yearId, "ACTIVE");
        Enrolment e2 = createEnrolment(s1.getId(), instB, yearId, "ACTIVE");

        when(studentRepository.findAll()).thenReturn(List.of(s1));
        when(enrolmentRepository.findAll()).thenReturn(List.of(e1, e2));

        when(runRepository.save(any(IdentityMatchingRun.class))).thenAnswer(i -> {
            IdentityMatchingRun run = i.getArgument(0);
            ReflectionTestUtils.setField(run, "id", UUID.randomUUID());
            return run;
        });
        when(conflictRepository.findFirstByFingerprintAndStatusIn(anyString(), any())).thenReturn(Optional.empty());
        when(conflictRepository.save(any(IdentityConflict.class))).thenAnswer(i -> {
            IdentityConflict c = i.getArgument(0);
            ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
            return c;
        });

        UserPrincipal actor = new UserPrincipal(
                UUID.randomUUID(), "admin@aicte.gov.in", "hash", "AICTE Officer", null,
                List.of("ROLE_AICTE_ADMIN"), List.of("CONFLICT_INVESTIGATE"), true, null
        );

        IdentityDtos.MatchingRunDto runDto = service.executeMatchingRun(
                new IdentityDtos.MatchingRunRequest(yearId, instA, "EXACT_MATCH"), actor);

        assertThat(runDto.matchesFound()).isEqualTo(1);
        assertThat(runDto.conflictsGenerated()).isEqualTo(1);
        assertThat(runDto.recordsScanned()).isEqualTo(3);
    }

    @Test
    @DisplayName("Historical sequential enrolment transition creates MATCH_ONLY without conflict")
    void testHistoricalSequentialEnrolmentTransition() {
        UUID instA = UUID.randomUUID();
        UUID instB = UUID.randomUUID();
        UUID year2024 = UUID.randomUUID();
        UUID year2025 = UUID.randomUUID();
        String apaar = "900000000002";

        Student s1 = createStudent("Student Beta", apaar, LocalDate.of(2003, 7, 20));

        Enrolment e1 = createEnrolment(s1.getId(), instA, year2024, "COMPLETED");
        Enrolment e2 = createEnrolment(s1.getId(), instB, year2025, "ACTIVE");

        when(studentRepository.findAll()).thenReturn(List.of(s1));
        when(enrolmentRepository.findAll()).thenReturn(List.of(e1, e2));

        when(runRepository.save(any(IdentityMatchingRun.class))).thenAnswer(i -> {
            IdentityMatchingRun run = i.getArgument(0);
            ReflectionTestUtils.setField(run, "id", UUID.randomUUID());
            return run;
        });

        UserPrincipal actor = new UserPrincipal(
                UUID.randomUUID(), "admin@aicte.gov.in", "hash", "AICTE Officer", null,
                List.of("ROLE_AICTE_ADMIN"), List.of("CONFLICT_INVESTIGATE"), true, null
        );
        IdentityDtos.MatchingRunRequest request = new IdentityDtos.MatchingRunRequest(null, null, "EXACT_MATCH");

        IdentityDtos.MatchingRunDto runDto = service.executeMatchingRun(request, actor);

        // Historical transition should create a match with reviewRequired=false and generate NO conflict!
        assertThat(runDto.conflictsGenerated()).isEqualTo(0);
        verify(conflictRepository, never()).save(any(IdentityConflict.class));
        verify(matchRepository, atLeastOnce()).saveAll(argThat(matches -> {
            for (IdentityMatch m : matches) {
                if ("POSSIBLE_HISTORICAL_ENROLMENT".equals(m.getMatchClassification()) && !m.isReviewRequired()) {
                    return true;
                }
            }
            return false;
        }));
    }

    @Test
    @DisplayName("Repeated run suppresses duplicate conflict via fingerprint")
    void testDuplicateConflictSuppression() {
        UUID instA = UUID.randomUUID();
        UUID instB = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        String apaar = "900000000003";

        Student s1 = createStudent("Student Gamma", apaar, LocalDate.of(2004, 1, 10));

        Enrolment e1 = createEnrolment(s1.getId(), instA, yearId, "ACTIVE");
        Enrolment e2 = createEnrolment(s1.getId(), instB, yearId, "ACTIVE");

        when(studentRepository.findAll()).thenReturn(List.of(s1));
        when(enrolmentRepository.findAll()).thenReturn(List.of(e1, e2));

        when(runRepository.save(any(IdentityMatchingRun.class))).thenAnswer(i -> {
            IdentityMatchingRun run = i.getArgument(0);
            ReflectionTestUtils.setField(run, "id", UUID.randomUUID());
            return run;
        });

        // Simulate an existing OPEN conflict with identical fingerprint
        IdentityConflict existingConflict = new IdentityConflict();
        ReflectionTestUtils.setField(existingConflict, "id", UUID.randomUUID());
        existingConflict.setStatus("OPEN");
        when(conflictRepository.findFirstByFingerprintAndStatusIn(anyString(), any())).thenReturn(Optional.of(existingConflict));

        UserPrincipal actor = new UserPrincipal(
                UUID.randomUUID(), "admin@aicte.gov.in", "hash", "AICTE Officer", null,
                List.of("ROLE_AICTE_ADMIN"), List.of("CONFLICT_INVESTIGATE"), true, null
        );
        IdentityDtos.MatchingRunDto runDto = service.executeMatchingRun(new IdentityDtos.MatchingRunRequest(null, null, "EXACT_MATCH"), actor);

        // Conflict should be suppressed!
        assertThat(runDto.conflictsSuppressed()).isGreaterThanOrEqualTo(1);
        assertThat(runDto.conflictsGenerated()).isEqualTo(0);
        verify(conflictRepository, never()).save(any(IdentityConflict.class));
    }

    @Test
    @DisplayName("Legacy conflict without fingerprint is suppressed via enrolment pair and backfilled")
    void testLegacyConflictSuppressedByEnrolmentPair() {
        UUID instA = UUID.randomUUID();
        UUID instB = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        String apaar = "900000000004";

        Student s1 = createStudent("Student Delta", apaar, LocalDate.of(2004, 5, 12));

        Enrolment e1 = createEnrolment(s1.getId(), instA, yearId, "ACTIVE");
        Enrolment e2 = createEnrolment(s1.getId(), instB, yearId, "ACTIVE");

        when(studentRepository.findAll()).thenReturn(List.of(s1));
        when(enrolmentRepository.findAll()).thenReturn(List.of(e1, e2));

        when(runRepository.save(any(IdentityMatchingRun.class))).thenAnswer(i -> {
            IdentityMatchingRun run = i.getArgument(0);
            ReflectionTestUtils.setField(run, "id", UUID.randomUUID());
            return run;
        });
        when(conflictRepository.findFirstByFingerprintAndStatusIn(anyString(), any())).thenReturn(Optional.empty());

        IdentityConflict legacyConflict = new IdentityConflict();
        ReflectionTestUtils.setField(legacyConflict, "id", UUID.randomUUID());
        legacyConflict.setStatus("OPEN");
        legacyConflict.setReasonCode("DUPLICATE_ACTIVE_ENROLMENT");
        when(conflictRepository.findOpenByEnrolments(eq("DUPLICATE_ACTIVE_ENROLMENT"), any(), any()))
                .thenReturn(Optional.of(legacyConflict));
        when(conflictRepository.save(any(IdentityConflict.class))).thenAnswer(i -> i.getArgument(0));

        UserPrincipal actor = new UserPrincipal(
                UUID.randomUUID(), "admin@aicte.gov.in", "hash", "AICTE Officer", null,
                List.of("ROLE_AICTE_ADMIN"), List.of("CONFLICT_INVESTIGATE"), true, null
        );

        IdentityDtos.MatchingRunDto runDto = service.executeMatchingRun(
                new IdentityDtos.MatchingRunRequest(null, null, "EXACT_MATCH"), actor);

        assertThat(runDto.conflictsGenerated()).isEqualTo(0);
        assertThat(runDto.conflictsSuppressed()).isGreaterThanOrEqualTo(1);
        assertThat(legacyConflict.getFingerprint()).isNotNull();
        verify(conflictRepository, times(1)).save(legacyConflict);
    }

    @Test
    @DisplayName("Dashboard counts distinct duplicate conflicts instead of repeated match rows")
    void testDashboardCountsDistinctDuplicateConflicts() {
        when(accessScope.isPlatformWide()).thenReturn(true);
        when(studentRepository.count()).thenReturn(10L);
        when(studentRepository.countApaarFormatValid(null)).thenReturn(6L);
        when(conflictRepository.countByStatusInAndInstitutionId(anyList(), isNull())).thenReturn(2L);
        when(verificationRecordRepository.countByStatusAndVerificationModeAndInstitutionId(
                "VERIFIED", "AUTHORIZED", null)).thenReturn(1L);
        when(verificationRecordRepository.countByStatusAndVerificationModeAndInstitutionId(
                "VERIFIED", "SIMULATED", null)).thenReturn(2L);
        when(verificationRecordRepository.countByStatusAndInstitutionId("VERIFIED", null)).thenReturn(3L);
        when(matchRepository.countDistinctConflictsByMatchClassification(
                "POTENTIAL_DUPLICATE_ENROLMENT", null)).thenReturn(3L);
        when(caseRepository.countByStatusInAndInstitutionId(anyList(), isNull())).thenReturn(0L);

        IdentityDtos.IdentityDashboardDto dashboard = service.getDashboardMetrics();

        assertThat(dashboard.potentialDuplicatesCount()).isEqualTo(3);
        assertThat(dashboard.openConflictsCount()).isEqualTo(2);
        verify(caseRepository).countByStatusInAndInstitutionId(
                eq(List.of("OPEN", "ASSIGNED", "IN_REVIEW", "PENDING_INFO")), isNull());
    }

    @Test
    @DisplayName("Dashboard metrics are scoped to the caller's institution")
    void testDashboardScopedToInstitution() {
        UUID inst = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(), "admin@institution.demo", "hash", "Institution Admin", inst,
                List.of("ROLE_INSTITUTION_ADMIN"), List.of("CONFLICT_READ"), true, null
        );

        when(accessScope.isPlatformWide()).thenReturn(false);
        when(accessScope.requirePrincipal()).thenReturn(principal);
        when(studentRepository.countDistinctByInstitutionId(inst)).thenReturn(4L);
        when(studentRepository.countApaarFormatValid(inst)).thenReturn(3L);
        when(conflictRepository.countByStatusInAndInstitutionId(anyList(), eq(inst))).thenReturn(1L);
        when(verificationRecordRepository.countByStatusAndVerificationModeAndInstitutionId(
                "VERIFIED", "AUTHORIZED", inst)).thenReturn(0L);
        when(verificationRecordRepository.countByStatusAndVerificationModeAndInstitutionId(
                "VERIFIED", "SIMULATED", inst)).thenReturn(1L);
        when(verificationRecordRepository.countByStatusAndInstitutionId("VERIFIED", inst)).thenReturn(1L);
        when(matchRepository.countDistinctConflictsByMatchClassification(
                "POTENTIAL_DUPLICATE_ENROLMENT", inst)).thenReturn(1L);
        when(caseRepository.countByStatusInAndInstitutionId(anyList(), eq(inst))).thenReturn(0L);

        IdentityDtos.IdentityDashboardDto dashboard = service.getDashboardMetrics();

        assertThat(dashboard.totalIdentitiesAssessed()).isEqualTo(4);
        assertThat(dashboard.formatValidCount()).isEqualTo(3);
        assertThat(dashboard.potentialDuplicatesCount()).isEqualTo(1);
        verify(studentRepository).countDistinctByInstitutionId(inst);
        verify(studentRepository, never()).count();
    }

    private Student createStudent(String name, String apaar, LocalDate dob) {
        Student s = new Student();
        ReflectionTestUtils.setField(s, "id", UUID.randomUUID());
        s.setFullName(name);
        s.setApaarId(apaar);
        s.setApaarFormatValid(true);
        s.setDateOfBirth(dob);
        s.setGender("FEMALE");
        s.setCategory("GENERAL");
        return s;
    }

    private Enrolment createEnrolment(UUID studentId, UUID instId, UUID yearId, String status) {
        Enrolment e = new Enrolment();
        ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
        e.setStudentId(studentId);
        e.setInstitutionId(instId);
        e.setAcademicYearId(yearId);
        e.setProgrammeId(UUID.randomUUID());
        e.setEnrolmentNumber("ENR-" + UUID.randomUUID().toString().substring(0, 8));
        e.setStatus(status);
        e.setAdmissionDate(LocalDate.of(2024, 8, 1));
        return e;
    }
}
