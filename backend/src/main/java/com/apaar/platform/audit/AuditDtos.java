package com.apaar.platform.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AuditDtos {
    public record AuditEntryDto(
            String id,
            String eventType,
            String action,
            String actor,
            UUID actorId,
            String actorRole,
            String actorEmail,
            String entityType,
            String entityId,
            String outcome,
            String ipAddress,
            Map<String, Object> details,
            Instant occurredAt,
            Instant createdAt
    ) {}
}

