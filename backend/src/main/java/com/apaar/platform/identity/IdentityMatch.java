package com.apaar.platform.identity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "identity_matches")
public class IdentityMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "match_type", nullable = false, length = 48)
    private String matchType;

    @Column(name = "matching_rule", nullable = false, length = 64)
    private String matchingRule;

    @Column(name = "primary_student_id")
    private UUID primaryStudentId;

    @Column(name = "secondary_student_id")
    private UUID secondaryStudentId;

    @Column(name = "primary_enrolment_id")
    private UUID primaryEnrolmentId;

    @Column(name = "secondary_enrolment_id")
    private UUID secondaryEnrolmentId;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidenceScore = new BigDecimal("1.0000");

    @Column(name = "match_classification", nullable = false, length = 48)
    private String matchClassification;

    @Column(name = "review_required", nullable = false)
    private boolean reviewRequired = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matching_fields", columnDefinition = "jsonb")
    private List<String> matchingFields = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conflicting_fields", columnDefinition = "jsonb")
    private List<String> conflictingFields = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> details = new LinkedHashMap<>();

    @Column(name = "conflict_id")
    private UUID conflictId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public IdentityMatch() {
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public void setRunId(UUID runId) { this.runId = runId; }
    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }
    public String getMatchingRule() { return matchingRule; }
    public void setMatchingRule(String matchingRule) { this.matchingRule = matchingRule; }
    public UUID getPrimaryStudentId() { return primaryStudentId; }
    public void setPrimaryStudentId(UUID primaryStudentId) { this.primaryStudentId = primaryStudentId; }
    public UUID getSecondaryStudentId() { return secondaryStudentId; }
    public void setSecondaryStudentId(UUID secondaryStudentId) { this.secondaryStudentId = secondaryStudentId; }
    public UUID getPrimaryEnrolmentId() { return primaryEnrolmentId; }
    public void setPrimaryEnrolmentId(UUID primaryEnrolmentId) { this.primaryEnrolmentId = primaryEnrolmentId; }
    public UUID getSecondaryEnrolmentId() { return secondaryEnrolmentId; }
    public void setSecondaryEnrolmentId(UUID secondaryEnrolmentId) { this.secondaryEnrolmentId = secondaryEnrolmentId; }
    public BigDecimal getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(BigDecimal confidenceScore) { this.confidenceScore = confidenceScore; }
    public String getMatchClassification() { return matchClassification; }
    public void setMatchClassification(String matchClassification) { this.matchClassification = matchClassification; }
    public boolean isReviewRequired() { return reviewRequired; }
    public void setReviewRequired(boolean reviewRequired) { this.reviewRequired = reviewRequired; }
    public List<String> getMatchingFields() { return matchingFields; }
    public void setMatchingFields(List<String> matchingFields) { this.matchingFields = matchingFields; }
    public List<String> getConflictingFields() { return conflictingFields; }
    public void setConflictingFields(List<String> conflictingFields) { this.conflictingFields = conflictingFields; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    public UUID getConflictId() { return conflictId; }
    public void setConflictId(UUID conflictId) { this.conflictId = conflictId; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

