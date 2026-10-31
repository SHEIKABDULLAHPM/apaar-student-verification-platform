package com.apaar.platform.apaar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "apaar_format_validations")
public class ApaarFormatValidation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "apaar_id", nullable = false, length = 12)
    private String apaarId;

    @Column(name = "is_valid", nullable = false)
    private boolean valid;

    @Column(name = "reason_code", nullable = false, length = 64)
    private String reasonCode;

    @Column(name = "validated_by")
    private UUID validatedBy;

    @Column(name = "validated_at", nullable = false)
    private Instant validatedAt;

    protected ApaarFormatValidation() {
    }

    public ApaarFormatValidation(UUID studentId, String apaarId, boolean valid, String reasonCode, UUID validatedBy) {
        this.studentId = studentId;
        this.apaarId = apaarId;
        this.valid = valid;
        this.reasonCode = reasonCode;
        this.validatedBy = validatedBy;
        this.validatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public String getApaarId() { return apaarId; }
    public boolean isValid() { return valid; }
    public String getReasonCode() { return reasonCode; }
    public UUID getValidatedBy() { return validatedBy; }
    public Instant getValidatedAt() { return validatedAt; }
}
