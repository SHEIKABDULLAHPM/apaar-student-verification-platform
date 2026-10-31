package com.apaar.platform.anomaly;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "anomaly_runs")
public class AnomalyRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "institution_id")
    private UUID institutionId;

    @Column(name = "academic_year_id")
    private UUID academicYearId;

    @Column(name = "model_name", nullable = false, length = 96)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 32)
    private String modelVersion;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(nullable = false, length = 24)
    private String status = "PENDING";

    @Column(name = "records_evaluated", nullable = false)
    private Integer recordsEvaluated = 0;

    @Column(name = "flagged_count", nullable = false)
    private Integer flaggedCount = 0;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    protected AnomalyRun() {
    }

    public UUID getId() { return id; }
    public UUID getInstitutionId() { return institutionId; }
    public UUID getAcademicYearId() { return academicYearId; }
    public String getModelName() { return modelName; }
    public String getModelVersion() { return modelVersion; }
    public UUID getRequestedBy() { return requestedBy; }
    public String getStatus() { return status; }
    public Integer getRecordsEvaluated() { return recordsEvaluated; }
    public Integer getFlaggedCount() { return flaggedCount; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}

