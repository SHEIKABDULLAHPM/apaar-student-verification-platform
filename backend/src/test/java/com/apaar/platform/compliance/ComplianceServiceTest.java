package com.apaar.platform.compliance;

import com.apaar.platform.academic.ProgrammeIntakeRepository;
import com.apaar.platform.academic.ProgrammeRepository;
import com.apaar.platform.audit.AuditService;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.compliance.ComplianceDtos.ComplianceViolationDto;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceServiceTest {

    @Mock private ComplianceViolationRepository violationRepository;
    @Mock private ComplianceEvaluationRepository evaluationRepository;
    @Mock private ProgrammeIntakeRepository intakeRepository;
    @Mock private ProgrammeRepository programmeRepository;
    @Mock private EnrolmentRepository enrolmentRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private AccessScopeService accessScope;
    @Mock private AuditService auditService;

    @InjectMocks
    private ComplianceService complianceService;

    @Test
    @DisplayName("listViolations returns paged violations")
    void listViolationsReturnsPage() {
        when(accessScope.isPlatformWide()).thenReturn(true);
        ComplianceViolation violation = new ComplianceViolation(
                UUID.randomUUID(), null, null, null, "INTAKE_EXCESS", "HIGH", Map.of("key", "val")
        );
        org.springframework.test.util.ReflectionTestUtils.setField(violation, "id", UUID.randomUUID());
        when(violationRepository.search(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(violation)));

        PageResponse<ComplianceViolationDto> response = complianceService.listViolations(0, 10, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).reasonCode()).isEqualTo("INTAKE_EXCESS");
    }
}
