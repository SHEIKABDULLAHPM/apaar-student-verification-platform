package com.apaar.platform.anomaly;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "anomaly_results")
public class AnomalyResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "entity_type", nullable = false, length = 32)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "enrolment_id")
    private UUID enrolmentId;

    @Column(name = "model_name", nullable = false, length = 96)
    private String modelName;

    @Column(name = "model_version", nullable = false, length = 32)
    private String modelVersion;

    @Column(name = "anomaly_score", nullable = false, precision = 6, scale = 4)
    private BigDecimal anomalyScore;

    @Column(nullable = false, precision = 6, scale = 4)
    private BigDecimal threshold;

    @Column(name = "is_anomaly", nullable = false)
    private Boolean anomaly = true;

    @Column(nullable = false, length = 16)
    private String severity;

    @Column(name = "reason_codes", columnDefinition = "text[]")
    private String[] reasonCodes = new String[0];

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Object explanations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> features = new LinkedHashMap<>();

    @Column(nullable = false, length = 24)
    private String status = "NEW";

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_notes", length = 2000)
    private String reviewNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AnomalyResult() {
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public UUID getStudentId() { return studentId; }
    public UUID getEnrolmentId() { return enrolmentId; }
    public String getModelName() { return modelName; }
    public String getModelVersion() { return modelVersion; }
    public BigDecimal getAnomalyScore() { return anomalyScore; }
    public BigDecimal getThreshold() { return threshold; }
    public Boolean getAnomaly() { return anomaly; }
    public String getSeverity() { return severity; }
    public String[] getReasonCodes() { return reasonCodes; }
    public Object getExplanations() { return explanations; }
    public Map<String, Object> getFeatures() { return features; }
    public String getStatus() { return status; }
    public UUID getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewNotes() { return reviewNotes; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
    public void setReviewedBy(UUID reviewedBy) { this.reviewedBy = reviewedBy; }
    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setReviewNotes(String reviewNotes) { this.reviewNotes = reviewNotes; }
}

