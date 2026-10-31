package com.apaar.platform.dashboard;

import com.apaar.platform.academic.AcademicYearRepository;
import com.apaar.platform.academic.ProgrammeIntakeRepository;
import com.apaar.platform.academic.ProgrammeRepository;
import com.apaar.platform.anomaly.AnomalyResultRepository;
import com.apaar.platform.cases.InvestigationCaseRepository;
import com.apaar.platform.compliance.ComplianceViolationRepository;
import com.apaar.platform.dashboard.DashboardDtos.DashboardSummaryDto;
import com.apaar.platform.identity.IdentityConflictRepository;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private InstitutionRepository institutionRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private EnrolmentRepository enrolmentRepository;
    @Mock private ProgrammeRepository programmeRepository;
    @Mock private ProgrammeIntakeRepository intakeRepository;
    @Mock private AcademicYearRepository academicYearRepository;
    @Mock private IdentityConflictRepository identityConflictRepository;
    @Mock private InvestigationCaseRepository caseRepository;
    @Mock private ComplianceViolationRepository complianceViolationRepository;
    @Mock private AnomalyResultRepository anomalyResultRepository;
    @Mock private AccessScopeService accessScope;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("getAicteDashboard returns aggregated counts")
    void getAicteDashboardAggregates() {
        when(institutionRepository.count()).thenReturn(5L);
        when(studentRepository.count()).thenReturn(50L);
        when(studentRepository.countApaarVerified()).thenReturn(40L);
        when(enrolmentRepository.countActive(isNull())).thenReturn(55L);
        when(identityConflictRepository.countByStatusIn(any())).thenReturn(2L);
        when(caseRepository.countByStatusInAndInstitutionId(any(), isNull())).thenReturn(3L);
        when(complianceViolationRepository.countByStatusIn(any())).thenReturn(1L);
        when(anomalyResultRepository.countByStatusIn(any())).thenReturn(4L);
        when(intakeRepository.findAll()).thenReturn(Collections.emptyList());
        when(enrolmentRepository.countActiveByCategory(isNull())).thenReturn(List.of(
                new Object[]{"GENERAL", 30L},
                new Object[]{"OBC", 25L}
        ));

        DashboardSummaryDto summary = dashboardService.getAicteDashboard();

        assertThat(summary).isNotNull();
        assertThat(summary.institutionCount()).isEqualTo(5L);
        assertThat(summary.studentCount()).isEqualTo(50);
        assertThat(summary.apaarVerifiedCount()).isEqualTo(40);
        assertThat(summary.apaarUnverifiedCount()).isEqualTo(10);
        assertThat(summary.enrolmentCount()).isEqualTo(55);
        assertThat(summary.openConflictCount()).isEqualTo(2);
        assertThat(summary.openCaseCount()).isEqualTo(3);
        assertThat(summary.complianceFindingCount()).isEqualTo(1);
        assertThat(summary.anomalyAlertCount()).isEqualTo(4);
        assertThat(summary.enrolmentsByCategory()).isNotEmpty();
    }
}

