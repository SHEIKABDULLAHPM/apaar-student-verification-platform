package com.apaar.platform.identity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "identity_matching_runs")
public class IdentityMatchingRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "run_type", nullable = false, length = 48)
    private String runType = "EXACT_MATCH";

    @Column(nullable = false, length = 24)
    private String status = "COMPLETED";

    @Column(name = "records_scanned", nullable = false)
    private int recordsScanned;

    @Column(name = "matches_found", nullable = false)
    private int matchesFound;

    @Column(name = "conflicts_generated", nullable = false)
    private int conflictsGenerated;

    @Column(name = "conflicts_suppressed", nullable = false)
    private int conflictsSuppressed;

    @Column(name = "run_by")
    private UUID runBy;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> summary = new LinkedHashMap<>();

    public IdentityMatchingRun() {
    }

    public IdentityMatchingRun(String runType, int recordsScanned, int matchesFound,
                               int conflictsGenerated, int conflictsSuppressed, UUID runBy,
                               Instant startedAt, Instant completedAt, Map<String, Object> summary) {
        this.runType = runType;
        this.recordsScanned = recordsScanned;
        this.matchesFound = matchesFound;
        this.conflictsGenerated = conflictsGenerated;
        this.conflictsSuppressed = conflictsSuppressed;
        this.runBy = runBy;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.summary = summary != null ? summary : new LinkedHashMap<>();
        this.status = "COMPLETED";
    }

    public UUID getId() { return id; }
    public String getRunType() { return runType; }
    public void setRunType(String runType) { this.runType = runType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getRecordsScanned() { return recordsScanned; }
    public void setRecordsScanned(int recordsScanned) { this.recordsScanned = recordsScanned; }
    public int getMatchesFound() { return matchesFound; }
    public void setMatchesFound(int matchesFound) { this.matchesFound = matchesFound; }
    public int getConflictsGenerated() { return conflictsGenerated; }
    public void setConflictsGenerated(int conflictsGenerated) { this.conflictsGenerated = conflictsGenerated; }
    public int getConflictsSuppressed() { return conflictsSuppressed; }
    public void setConflictsSuppressed(int conflictsSuppressed) { this.conflictsSuppressed = conflictsSuppressed; }
    public UUID getRunBy() { return runBy; }
    public void setRunBy(UUID runBy) { this.runBy = runBy; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Map<String, Object> getSummary() { return summary; }
    public void setSummary(Map<String, Object> summary) { this.summary = summary; }
}

