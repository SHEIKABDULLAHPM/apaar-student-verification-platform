package com.apaar.platform.security;

import com.apaar.platform.common.exception.ForbiddenOperationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessScopeServiceTest {

    private final AccessScopeService service = new AccessScopeService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(List<String> roles, UUID institutionId) {
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(),
                "test@user.local",
                "hash",
                "Test User",
                institutionId,
                roles,
                Collections.emptySet(),
                true,
                null
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("Platform-wide roles can filter by any requested institution")
    void platformAdminScope() {
        authenticate(List.of("PLATFORM_ADMIN"), null);
        UUID target = UUID.randomUUID();
        UUID resolved = service.resolveInstitutionFilter(target);
        assertThat(resolved).isEqualTo(target);
    }

    @Test
    @DisplayName("AICTE Officer is platform-wide and not constrained to an institution")
    void aicteOfficerScope() {
        authenticate(List.of("AICTE_OFFICER"), null);
        assertThat(service.isPlatformWide()).isTrue();
    }

    @Test
    @DisplayName("Institution user requesting their own institution succeeds")
    void institutionOwnScope() {
        UUID ownInst = UUID.randomUUID();
        authenticate(List.of("INSTITUTION_ADMIN"), ownInst);

        UUID resolved = service.resolveInstitutionFilter(ownInst);
        assertThat(resolved).isEqualTo(ownInst);
    }

    @Test
    @DisplayName("Institution user requesting another institution throws ForbiddenOperationException")
    void institutionAccessOtherDenied() {
        UUID ownInst = UUID.randomUUID();
        UUID otherInst = UUID.randomUUID();
        authenticate(List.of("INSTITUTION_ADMIN"), ownInst);

        assertThatThrownBy(() -> service.resolveInstitutionFilter(otherInst))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Access to another institution's data is not permitted");
    }

    @Test
    @DisplayName("Institution user with null requestedId resolves to their own institution")
    void institutionDefaultToOwn() {
        UUID ownInst = UUID.randomUUID();
        authenticate(List.of("INSTITUTION_OPERATOR"), ownInst);

        UUID resolved = service.resolveInstitutionFilter(null);
        assertThat(resolved).isEqualTo(ownInst);
    }
}

