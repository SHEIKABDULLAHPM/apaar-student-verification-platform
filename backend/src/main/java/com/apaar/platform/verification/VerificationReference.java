package com.apaar.platform.verification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_references")
public class VerificationReference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "public_ref", nullable = false, unique = true, length = 64)
    private String publicRef;

    @Column(name = "academic_record_id")
    private UUID academicRecordId;

    @Column(name = "enrolment_id", nullable = false)
    private UUID enrolmentId;

    @Column(nullable = false, length = 255)
    private String purpose;

    @Column(nullable = false, length = 24)
    private String status = "ACTIVE";

    @Column(name = "issued_by", nullable = false)
    private UUID issuedBy;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "max_uses", nullable = false)
    private int maxUses = 10;

    @Column(name = "use_count", nullable = false)
    private int useCount = 0;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected VerificationReference() {
    }

    public UUID getId() { return id; }
    public String getPublicRef() { return publicRef; }
    public UUID getAcademicRecordId() { return academicRecordId; }
    public UUID getEnrolmentId() { return enrolmentId; }
    public String getPurpose() { return purpose; }
    public String getStatus() { return status; }
    public UUID getIssuedBy() { return issuedBy; }
    public Instant getIssuedAt() { return issuedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public int getMaxUses() { return maxUses; }
    public int getUseCount() { return useCount; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setUseCount(int useCount) { this.useCount = useCount; }
    public void setStatus(String status) { this.status = status; }
}

