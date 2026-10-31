package com.apaar.platform.audit;

import com.apaar.platform.common.config.AuditProperties;
import com.apaar.platform.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository repository;

    private AuditService auditService;

    @BeforeEach
    void setUp() {
        AuditProperties properties = new AuditProperties("test-salt-12345");
        auditService = new AuditService(repository, properties);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Audit event records actor, action, outcome and entity")
    void recordAuditEvent() {
        UUID userId = UUID.randomUUID();
        UUID instId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                userId, "actor@test.local", "hash", "Actor", instId,
                List.of("INSTITUTION_ADMIN"), Collections.emptySet(), true, null
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        auditService.record("STUDENT_CREATED", "STUDENT", "123", "CREATE", "SUCCESS", instId, Map.of("key", "val"));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("STUDENT_CREATED");
        assertThat(saved.getActorId()).isEqualTo(userId);
        assertThat(saved.getActorRole()).isEqualTo("INSTITUTION_ADMIN");
        assertThat(saved.getAction()).isEqualTo("CREATE");
        assertThat(saved.getOutcome()).isEqualTo("SUCCESS");
        assertThat(saved.getInstitutionId()).isEqualTo(instId);
    }
}

