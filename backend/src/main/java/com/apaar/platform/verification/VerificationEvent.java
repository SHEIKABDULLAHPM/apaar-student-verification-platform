package com.apaar.platform.verification;

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
@Table(name = "verification_events")
public class VerificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "verification_reference_id", nullable = false)
    private UUID verificationReferenceId;

    @Column(name = "event_type", nullable = false, length = 32)
    private String eventType;

    @Column(name = "actor_type", nullable = false, length = 24)
    private String actorType;

    @Column(name = "actor_id", length = 128)
    private String actorId;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(nullable = false, length = 32)
    private String result;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> details = new LinkedHashMap<>();

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();

    protected VerificationEvent() {
    }

    public VerificationEvent(UUID verificationReferenceId, String eventType, String actorType,
                             String actorId, String result, Map<String, Object> details) {
        this.verificationReferenceId = verificationReferenceId;
        this.eventType = eventType;
        this.actorType = actorType;
        this.actorId = actorId;
        this.result = result;
        this.details = details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details);
        this.occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getVerificationReferenceId() { return verificationReferenceId; }
    public String getEventType() { return eventType; }
    public String getActorType() { return actorType; }
    public String getActorId() { return actorId; }
    public String getResult() { return result; }
    public Map<String, Object> getDetails() { return details; }
    public Instant getOccurredAt() { return occurredAt; }
}

