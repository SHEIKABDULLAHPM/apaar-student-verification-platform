package com.apaar.platform.audit;

import com.apaar.platform.common.config.AuditProperties;
import com.apaar.platform.common.web.RequestIds;
import com.apaar.platform.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository repository;
    private final AuditProperties properties;
    private final com.apaar.platform.security.UserRepository userRepository;
    private final com.apaar.platform.security.AccessScopeService accessScope;

    public AuditService(AuditEventRepository repository, AuditProperties properties) {
        this(repository, properties, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuditService(AuditEventRepository repository,
                        AuditProperties properties,
                        com.apaar.platform.security.UserRepository userRepository,
                        com.apaar.platform.security.AccessScopeService accessScope) {
        this.repository = repository;
        this.properties = properties;
        this.userRepository = userRepository;
        this.accessScope = accessScope;
    }

    @Transactional(readOnly = true)
    public com.apaar.platform.common.api.PageResponse<AuditDtos.AuditEntryDto> listAuditEntries(int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        UUID institutionFilter = null;
        if (accessScope != null && !accessScope.isPlatformWide()) {
            institutionFilter = accessScope.requirePrincipal().getInstitutionId();
        }
        org.springframework.data.domain.Page<AuditEvent> eventPage = repository.search(institutionFilter, pageable);
        return com.apaar.platform.common.api.PageResponse.from(eventPage, this::toDto);
    }

    private AuditDtos.AuditEntryDto toDto(AuditEvent e) {
        String actorEmail = null;
        if (e.getActorId() != null && userRepository != null) {
            actorEmail = userRepository.findById(e.getActorId())
                    .map(com.apaar.platform.security.User::getEmail)
                    .orElse(null);
        }
        String actorDisplay = actorEmail != null ? actorEmail : (e.getActorRole() != null ? e.getActorRole() : "SYSTEM");
        return new AuditDtos.AuditEntryDto(
                String.valueOf(e.getId()),
                e.getEventType(),
                e.getAction(),
                actorDisplay,
                e.getActorId(),
                e.getActorRole(),
                actorEmail,
                e.getEntityType(),
                e.getEntityId(),
                e.getOutcome(),
                e.getIpHash(),
                e.getMetadata(),
                e.getOccurredAt(),
                e.getOccurredAt()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String eventType, String entityType, String entityId, String action, String outcome,
                       UUID institutionId, Map<String, Object> metadata) {
        UserPrincipal actor = currentActor();
        repository.save(new AuditEvent(eventType,
                actor == null ? null : actor.getUserId(),
                actor == null ? null : String.join(",", actor.getRoles()),
                institutionId,
                entityType,
                entityId,
                action,
                outcome,
                RequestIds.current(),
                clientIpHash(),
                metadata));
    }

    public void success(String eventType, String entityType, String entityId, String action, UUID institutionId) {
        record(eventType, entityType, entityId, action, "SUCCESS", institutionId, Map.of());
    }

    private UserPrincipal currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    private String clientIpHash() {
        return RequestIds.currentRequest().map(HttpServletRequest::getRemoteAddr).map(ip -> {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                digest.update(properties.ipHashSalt().getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(digest.digest(ip.getBytes(StandardCharsets.UTF_8)));
            } catch (NoSuchAlgorithmException e) {
                log.warn("SHA-256 unavailable; cannot hash client IP");
                return null;
            }
        }).orElse(null);
    }
}
