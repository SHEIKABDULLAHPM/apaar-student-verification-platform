package com.apaar.platform.anomaly;

import com.apaar.platform.anomaly.AnomalyDtos.AnomalyDto;
import com.apaar.platform.audit.AuditService;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.student.EnrolmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyServiceTest {

    @Mock private AnomalyResultRepository anomalyRepository;
    @Mock private AnomalyRunRepository runRepository;
    @Mock private EnrolmentRepository enrolmentRepository;
    @Mock private InstitutionRepository institutionRepository;
    @Mock private AccessScopeService accessScope;
    @Mock private AuditService auditService;

    @InjectMocks
    private AnomalyService anomalyService;

    @Test
    @DisplayName("listAnomalies returns mapped anomaly page")
    void listAnomaliesReturnsPage() {
        when(accessScope.isPlatformWide()).thenReturn(true);

        AnomalyResult result = new AnomalyResult();
        // inject fields or verify default fields
        org.springframework.test.util.ReflectionTestUtils.setField(result, "id", java.util.UUID.randomUUID());
        org.springframework.test.util.ReflectionTestUtils.setField(result, "entityType", "ENROLMENT");
        org.springframework.test.util.ReflectionTestUtils.setField(result, "modelName", "apaar-anomaly-model");
        org.springframework.test.util.ReflectionTestUtils.setField(result, "anomalyScore", new BigDecimal("0.8500"));
        org.springframework.test.util.ReflectionTestUtils.setField(result, "threshold", new BigDecimal("0.6500"));
        org.springframework.test.util.ReflectionTestUtils.setField(result, "severity", "HIGH");
        org.springframework.test.util.ReflectionTestUtils.setField(result, "status", "NEW");
        org.springframework.test.util.ReflectionTestUtils.setField(result, "reasonCodes", new String[]{"DUPLICATE_ENROLMENT_SIGNAL"});

        when(anomalyRepository.search(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(result)));

        PageResponse<AnomalyDto> response = anomalyService.listAnomalies(0, 10, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).anomalyType()).isEqualTo("DUPLICATE_ENROLMENT_SIGNAL");
        assertThat(response.content().get(0).severity()).isEqualTo("HIGH");
    }
}

