package com.apaar.platform.security;

import com.apaar.platform.common.exception.ForbiddenOperationException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Central institution-isolation guard. Controllers/services must call this rather than
 * trusting an institutionId supplied by the client.
 */
@Service
public class AccessScopeService {
    private static final List<String> PLATFORM_WIDE_ROLES =
            List.of("PLATFORM_ADMIN", "AICTE_OFFICER", "AUDITOR", "CASE_REVIEWER");

    public UserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required");
        }
        return principal;
    }

    public boolean isPlatformWide() {
        return requirePrincipal().getRoles().stream().anyMatch(PLATFORM_WIDE_ROLES::contains);
    }

    /**
     * Resolves the effective institution filter. Institution-scoped users are always forced
     * to their own institution; requesting another institution is rejected.
     */
    public UUID resolveInstitutionFilter(UUID requestedInstitutionId) {
        UserPrincipal principal = requirePrincipal();
        if (isPlatformWide()) {
            return requestedInstitutionId;
        }
        UUID own = principal.getInstitutionId();
        if (own == null) {
            throw new ForbiddenOperationException("INSTITUTION_SCOPE_REQUIRED", "This account is not assigned to an institution");
        }
        if (requestedInstitutionId != null && !own.equals(requestedInstitutionId)) {
            throw new ForbiddenOperationException("INSTITUTION_ACCESS_DENIED", "Access to another institution's data is not permitted");
        }
        return own;
    }

    public void assertInstitutionAccess(UUID institutionId) {
        UserPrincipal principal = requirePrincipal();
        if (isPlatformWide()) {
            return;
        }
        if (principal.getInstitutionId() == null || !principal.getInstitutionId().equals(institutionId)) {
            throw new ForbiddenOperationException("INSTITUTION_ACCESS_DENIED", "Access to another institution's data is not permitted");
        }
    }

    /** For writes, institution users may only write to their own institution. */
    public UUID requireWritableInstitution(UUID requestedInstitutionId) {
        UserPrincipal principal = requirePrincipal();
        if (isPlatformWide()) {
            if (requestedInstitutionId == null) {
                throw new ForbiddenOperationException("INSTITUTION_REQUIRED", "An institutionId is required");
            }
            return requestedInstitutionId;
        }
        UUID own = principal.getInstitutionId();
        if (own == null) {
            throw new ForbiddenOperationException("INSTITUTION_SCOPE_REQUIRED", "This account is not assigned to an institution");
        }
        if (requestedInstitutionId != null && !own.equals(requestedInstitutionId)) {
            throw new ForbiddenOperationException("INSTITUTION_ACCESS_DENIED", "Access to another institution's data is not permitted");
        }
        return own;
    }

    public boolean hasPermission(String permission) {
        return requirePrincipal().getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(permission));
    }
}
