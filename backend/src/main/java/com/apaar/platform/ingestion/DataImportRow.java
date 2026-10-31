package com.apaar.platform.ingestion;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "data_import_rows")
public class DataImportRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "import_job_id", nullable = false)
    private UUID importJobId;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "normalized_payload", columnDefinition = "jsonb")
    private Map<String, Object> normalizedPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_data", columnDefinition = "jsonb")
    private Map<String, String> rawData;

    @Column(name = "row_status", nullable = false, length = 24)
    private String rowStatus = "PENDING";

    @Column(name = "reconciliation_action", nullable = false, length = 32)
    private String reconciliationAction = "NEW";

    @Column(name = "matched_enrolment_id")
    private UUID matchedEnrolmentId;

    @Column(name = "error_count", nullable = false)
    private int errorCount = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public DataImportRow() {}

    public DataImportRow(UUID importJobId, int rowNumber, Map<String, String> rawData) {
        this.importJobId = importJobId;
        this.rowNumber = rowNumber;
        this.rawData = rawData;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getImportJobId() { return importJobId; }
    public void setImportJobId(UUID importJobId) { this.importJobId = importJobId; }
    public int getRowNumber() { return rowNumber; }
    public void setRowNumber(int rowNumber) { this.rowNumber = rowNumber; }
    public Map<String, Object> getNormalizedPayload() { return normalizedPayload; }
    public void setNormalizedPayload(Map<String, Object> normalizedPayload) { this.normalizedPayload = normalizedPayload; }
    public Map<String, String> getRawData() { return rawData; }
    public void setRawData(Map<String, String> rawData) { this.rawData = rawData; }
    public String getRowStatus() { return rowStatus; }
    public void setRowStatus(String rowStatus) { this.rowStatus = rowStatus; }
    public String getReconciliationAction() { return reconciliationAction; }
    public void setReconciliationAction(String reconciliationAction) { this.reconciliationAction = reconciliationAction; }
    public UUID getMatchedEnrolmentId() { return matchedEnrolmentId; }
    public void setMatchedEnrolmentId(UUID matchedEnrolmentId) { this.matchedEnrolmentId = matchedEnrolmentId; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

