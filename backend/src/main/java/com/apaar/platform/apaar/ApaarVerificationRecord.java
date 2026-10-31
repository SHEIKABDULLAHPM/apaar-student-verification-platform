package com.apaar.platform.apaar;

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
@Table(name = "apaar_verification_records")
public class ApaarVerificationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "apaar_id_submitted", nullable = false, length = 12)
    private String apaarIdSubmitted;

    @Column(name = "verification_mode", nullable = false, length = 24)
    private String verificationMode;

    @Column(name = "adapter_name", nullable = false, length = 96)
    private String adapterName;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "provider_reference", length = 128)
    private String providerReference;

    @Column(name = "reason_code", length = 64)
    private String reasonCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private Map<String, Object> responsePayload = new LinkedHashMap<>();

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApaarVerificationRecord() {
    }

    public static ApaarVerificationRecord create(UUID studentId, String apaarId, String mode, String adapterName,
                                                 String status, String providerReference, String reasonCode,
                                                 Map<String, Object> payload, UUID requestedBy, Instant verifiedAt,
                                                 Instant expiresAt) {
        ApaarVerificationRecord r = new ApaarVerificationRecord();
        r.studentId = studentId;
        r.apaarIdSubmitted = apaarId;
        r.verificationMode = mode;
        r.adapterName = adapterName;
        r.status = status;
        r.providerReference = providerReference;
        r.reasonCode = reasonCode;
        r.responsePayload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        r.requestedBy = requestedBy;
        r.requestedAt = Instant.now();
        r.verifiedAt = verifiedAt;
        r.expiresAt = expiresAt;
        r.createdAt = Instant.now();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public String getApaarIdSubmitted() { return apaarIdSubmitted; }
    public String getApaarId() { return apaarIdSubmitted; }
    public boolean isSimulated() {
        return "SIMULATED".equalsIgnoreCase(verificationMode)
                || (responsePayload != null && Boolean.TRUE.equals(responsePayload.get("simulated")));
    }
    public boolean isAuthoritative() {
        return !isSimulated() && ("AUTHORIZED".equalsIgnoreCase(verificationMode) || "AUTHORITATIVE".equalsIgnoreCase(verificationMode));
    }
    public String getVerificationMode() { return verificationMode; }
    public String getAdapterName() { return adapterName; }
    public String getStatus() { return status; }
    public String getProviderReference() { return providerReference; }
    public String getReasonCode() { return reasonCode; }
    public Map<String, Object> getResponsePayload() { return responsePayload; }
    public UUID getRequestedBy() { return requestedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
