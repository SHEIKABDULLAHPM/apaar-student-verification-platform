package com.apaar.platform.academic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "programme_intakes")
public class ProgrammeIntake {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "programme_id", nullable = false)
    private UUID programmeId;

    @Column(name = "academic_year_id", nullable = false)
    private UUID academicYearId;

    @Column(name = "approved_intake", nullable = false)
    private int approvedIntake;

    @Column(name = "supernumerary_intake", nullable = false)
    private int supernumeraryIntake;

    @Column(name = "nri_intake", nullable = false)
    private int nriIntake;

    @Column(name = "oci_intake", nullable = false)
    private int ociIntake;

    @Column(name = "foreign_national_intake", nullable = false)
    private int foreignNationalIntake;

    @Column(nullable = false, length = 24)
    private String status = "ACTIVE";

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getProgrammeId() { return programmeId; }
    public void setProgrammeId(UUID programmeId) { this.programmeId = programmeId; }
    public UUID getAcademicYearId() { return academicYearId; }
    public void setAcademicYearId(UUID academicYearId) { this.academicYearId = academicYearId; }
    public int getApprovedIntake() { return approvedIntake; }
    public void setApprovedIntake(int approvedIntake) { this.approvedIntake = approvedIntake; }
    public int getSupernumeraryIntake() { return supernumeraryIntake; }
    public void setSupernumeraryIntake(int supernumeraryIntake) { this.supernumeraryIntake = supernumeraryIntake; }
    public int getNriIntake() { return nriIntake; }
    public void setNriIntake(int nriIntake) { this.nriIntake = nriIntake; }
    public int getOciIntake() { return ociIntake; }
    public void setOciIntake(int ociIntake) { this.ociIntake = ociIntake; }
    public int getForeignNationalIntake() { return foreignNationalIntake; }
    public void setForeignNationalIntake(int foreignNationalIntake) { this.foreignNationalIntake = foreignNationalIntake; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
