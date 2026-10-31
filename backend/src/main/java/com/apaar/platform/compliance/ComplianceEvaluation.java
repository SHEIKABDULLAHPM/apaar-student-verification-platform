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
@Table(name = "compliance_evaluations")
public class ComplianceEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "institution_id")
    private UUID institutionId;

    @Column(name = "academic_year_id")
    private UUID academicYearId;

    @Column(nullable = false, length = 24)
    private String status = "PENDING";

    @Column(name = "ruleset_version", nullable = false)
    private Integer rulesetVersion = 1;

    @Column(name = "evaluated_by")
    private UUID evaluatedBy;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    @Column(name = "total_findings", nullable = false)
    private Integer totalFindings = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> summary = new LinkedHashMap<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected ComplianceEvaluation() {
    }

    public ComplianceEvaluation(UUID institutionId, UUID academicYearId, UUID evaluatedBy) {
        this.institutionId = institutionId;
        this.academicYearId = academicYearId;
        this.evaluatedBy = evaluatedBy;
        this.status = "COMPLETED";
        this.rulesetVersion = 1;
        this.evaluatedAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getInstitutionId() { return institutionId; }
    public UUID getAcademicYearId() { return academicYearId; }
    public String getStatus() { return status; }
    public Integer getRulesetVersion() { return rulesetVersion; }
    public UUID getEvaluatedBy() { return evaluatedBy; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public Integer getTotalFindings() { return totalFindings; }
    public Map<String, Object> getSummary() { return summary; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(String status) { this.status = status; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public void setTotalFindings(Integer totalFindings) { this.totalFindings = totalFindings; }
    public void setSummary(Map<String, Object> summary) { this.summary = summary == null ? new LinkedHashMap<>() : summary; }
}

