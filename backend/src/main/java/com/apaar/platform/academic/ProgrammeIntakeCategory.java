package com.apaar.platform.academic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "programme_intake_categories")
public class ProgrammeIntakeCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "programme_intake_id", nullable = false)
    private UUID programmeIntakeId;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(name = "approved_seats", nullable = false)
    private int approvedSeats;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getProgrammeIntakeId() { return programmeIntakeId; }
    public void setProgrammeIntakeId(UUID programmeIntakeId) { this.programmeIntakeId = programmeIntakeId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getApprovedSeats() { return approvedSeats; }
    public void setApprovedSeats(int approvedSeats) { this.approvedSeats = approvedSeats; }
}
