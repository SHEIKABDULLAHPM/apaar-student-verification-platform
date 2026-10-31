package com.apaar.platform.compliance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "compliance_violations")
public class ComplianceViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "evaluation_id", nullable = false)
    private UUID evaluationId;

    @Column(name = "rule_id")
    private UUID ruleId;

    @Column(name = "enrolment_id")
    private UUID enrolmentId;

    @Column(name = "student_id")
    private UUID studentId;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(nullable = false, length = 16)
    private String severity;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> details = new LinkedHashMap<>();

    @Column(nullable = false, length = 24)
    private String status = "OPEN";

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ComplianceViolation() {
    }

    public ComplianceViolation(UUID evaluationId, UUID ruleId, UUID enrolmentId, UUID studentId,
                               String reasonCode, String severity, Map<String, Object> details) {
        this.evaluationId = evaluationId;
        this.ruleId = ruleId;
        this.enrolmentId = enrolmentId;
        this.studentId = studentId;
        this.reasonCode = reasonCode;
        this.severity = severity;
        this.details = details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details);
        this.status = "OPEN";
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getEvaluationId() { return evaluationId; }
    public UUID getRuleId() { return ruleId; }
    public UUID getEnrolmentId() { return enrolmentId; }
    public UUID getStudentId() { return studentId; }
    public String getReasonCode() { return reasonCode; }
    public String getSeverity() { return severity; }
    public Map<String, Object> getDetails() { return details; }
    public String getStatus() { return status; }
    public UUID getResolvedBy() { return resolvedBy; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}

