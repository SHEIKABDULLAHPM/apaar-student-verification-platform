package com.apaar.platform.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class UserPrincipal implements UserDetails {
    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final UUID institutionId;
    private final List<String> roles;
    private final Set<GrantedAuthority> authorities;
    private final boolean enabled;
    private final Instant lockedUntil;

    public UserPrincipal(UUID userId, String email, String passwordHash, String fullName, UUID institutionId,
                         List<String> roles, Collection<String> permissions, boolean enabled, Instant lockedUntil) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.institutionId = institutionId;
        this.roles = List.copyOf(roles);
        Set<GrantedAuthority> granted = new LinkedHashSet<>();
        roles.forEach(r -> granted.add(new SimpleGrantedAuthority("ROLE_" + r)));
        permissions.forEach(p -> granted.add(new SimpleGrantedAuthority(p)));
        this.authorities = Set.copyOf(granted);
        this.enabled = enabled;
        this.lockedUntil = lockedUntil;
    }

    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public UUID getInstitutionId() { return institutionId; }
    public List<String> getRoles() { return roles; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return lockedUntil == null || lockedUntil.isBefore(Instant.now()); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return enabled; }
}
