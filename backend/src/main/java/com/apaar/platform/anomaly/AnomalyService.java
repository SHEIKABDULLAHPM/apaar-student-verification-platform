package com.apaar.platform.anomaly;

import com.apaar.platform.anomaly.AnomalyDtos.AnomalyDto;
import com.apaar.platform.anomaly.AnomalyDtos.AnomalyReviewInputDto;
import com.apaar.platform.audit.AuditService;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.institution.Institution;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class AnomalyService {

    private final AnomalyResultRepository anomalyRepository;
    private final AnomalyRunRepository runRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final InstitutionRepository institutionRepository;
    private final AccessScopeService accessScope;
    private final AuditService auditService;

    public AnomalyService(AnomalyResultRepository anomalyRepository,
                          AnomalyRunRepository runRepository,
                          EnrolmentRepository enrolmentRepository,
                          InstitutionRepository institutionRepository,
                          AccessScopeService accessScope,
                          AuditService auditService) {
        this.anomalyRepository = anomalyRepository;
        this.runRepository = runRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.institutionRepository = institutionRepository;
        this.accessScope = accessScope;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AnomalyDto> listAnomalies(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        UUID institutionFilter = null;
        if (!accessScope.isPlatformWide()) {
            institutionFilter = accessScope.requirePrincipal().getInstitutionId();
        }

        Page<AnomalyResult> anomalyPage = anomalyRepository.search(institutionFilter, cleanStatus, pageable);
        return PageResponse.from(anomalyPage, this::toDto);
    }

    @Transactional
    public AnomalyDto reviewAnomaly(UUID id, AnomalyReviewInputDto input) {
        AnomalyResult anomaly = anomalyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ANOMALY_NOT_FOUND", "Anomaly result not found: " + id));

        UUID reviewerId = accessScope.requirePrincipal().getUserId();
        anomaly.setStatus(input.decision().trim().toUpperCase());
        anomaly.setReviewedBy(reviewerId);
        anomaly.setReviewedAt(Instant.now());
        anomaly.setReviewNotes(input.notes());

        AnomalyResult updated = anomalyRepository.save(anomaly);

        auditService.record("ANOMALY_REVIEWED", "ANOMALY_RESULT",
                id.toString(), "REVIEW", "SUCCESS", null,
                Map.of("decision", input.decision(), "notes", input.notes() == null ? "" : input.notes()));

        return toDto(updated);
    }

    private AnomalyDto toDto(AnomalyResult a) {
        String institutionName = null;
        if (a.getEnrolmentId() != null) {
            Enrolment e = enrolmentRepository.findById(a.getEnrolmentId()).orElse(null);
            if (e != null && e.getInstitutionId() != null) {
                institutionName = institutionRepository.findById(e.getInstitutionId())
                        .map(Institution::getName)
                        .orElse(null);
            }
        }
        if (institutionName == null && a.getRunId() != null) {
            AnomalyRun r = runRepository.findById(a.getRunId()).orElse(null);
            if (r != null && r.getInstitutionId() != null) {
                institutionName = institutionRepository.findById(r.getInstitutionId())
                        .map(Institution::getName)
                        .orElse(null);
            }
        }

        String anomalyType = (a.getReasonCodes() != null && a.getReasonCodes().length > 0)
                ? a.getReasonCodes()[0]
                : (a.getModelName() != null ? a.getModelName() : "STATISTICAL_OUTLIER");

        String description = "Statistical & ML anomaly flagged for review (Score: " + a.getAnomalyScore() + ")";
        if (a.getReasonCodes() != null && a.getReasonCodes().length > 0) {
            description = String.join(", ", a.getReasonCodes()) + " - Score: " + a.getAnomalyScore();
        }

        return new AnomalyDto(
                a.getId().toString(),
                a.getEntityType(),
                anomalyType,
                a.getSeverity(),
                a.getStatus(),
                description,
                institutionName,
                a.getStudentId() != null ? a.getStudentId().toString() : null,
                a.getCreatedAt()
        );
    }
}

