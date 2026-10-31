package com.apaar.platform.audit;

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
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_role", length = 64)
    private String actorRole;

    @Column(name = "institution_id")
    private UUID institutionId;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(nullable = false, length = 48)
    private String action;

    @Column(nullable = false, length = 24)
    private String outcome;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new LinkedHashMap<>();

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditEvent() {
    }

    public AuditEvent(String eventType, UUID actorId, String actorRole, UUID institutionId, String entityType,
                      String entityId, String action, String outcome, String requestId, String ipHash,
                      Map<String, Object> metadata) {
        this.eventType = eventType;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.institutionId = institutionId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.outcome = outcome;
        this.requestId = requestId;
        this.ipHash = ipHash;
        this.metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        this.occurredAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getEventType() { return eventType; }
    public UUID getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public UUID getInstitutionId() { return institutionId; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getAction() { return action; }
    public String getOutcome() { return outcome; }
    public String getRequestId() { return requestId; }
    public String getIpHash() { return ipHash; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getOccurredAt() { return occurredAt; }
}
