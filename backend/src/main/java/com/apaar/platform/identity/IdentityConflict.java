package com.apaar.platform.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "identity_conflicts")
public class IdentityConflict {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conflict_type", nullable = false, length = 48)
    private String conflictType;

    @Column(name = "primary_student_id")
    private UUID primaryStudentId;

    @Column(name = "secondary_student_id")
    private UUID secondaryStudentId;

    @Column(name = "primary_enrolment_id")
    private UUID primaryEnrolmentId;

    @Column(name = "secondary_enrolment_id")
    private UUID secondaryEnrolmentId;

    @Column(name = "detection_source", nullable = false, length = 24)
    private String detectionSource = "RULE";

    @Column(nullable = false, length = 24)
    private String status = "OPEN";

    @Column(nullable = false, length = 16)
    private String severity = "MEDIUM";

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> details = new LinkedHashMap<>();

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolution_notes", length = 2000)
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(length = 128)
    private String fingerprint;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public UUID getId() { return id; }
    public String getConflictType() { return conflictType; }
    public void setConflictType(String conflictType) { this.conflictType = conflictType; }
    public UUID getPrimaryStudentId() { return primaryStudentId; }
    public void setPrimaryStudentId(UUID primaryStudentId) { this.primaryStudentId = primaryStudentId; }
    public UUID getSecondaryStudentId() { return secondaryStudentId; }
    public void setSecondaryStudentId(UUID secondaryStudentId) { this.secondaryStudentId = secondaryStudentId; }
    public UUID getPrimaryEnrolmentId() { return primaryEnrolmentId; }
    public void setPrimaryEnrolmentId(UUID primaryEnrolmentId) { this.primaryEnrolmentId = primaryEnrolmentId; }
    public UUID getSecondaryEnrolmentId() { return secondaryEnrolmentId; }
    public void setSecondaryEnrolmentId(UUID secondaryEnrolmentId) { this.secondaryEnrolmentId = secondaryEnrolmentId; }
    public String getDetectionSource() { return detectionSource; }
    public void setDetectionSource(String detectionSource) { this.detectionSource = detectionSource; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
    public UUID getAssignedTo() { return assignedTo; }
    public void setAssignedTo(UUID assignedTo) { this.assignedTo = assignedTo; }
    public UUID getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(UUID resolvedBy) { this.resolvedBy = resolvedBy; }
    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
