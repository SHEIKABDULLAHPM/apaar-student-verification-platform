package com.apaar.platform.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserPrincipal loadById(UUID id) {
        User user = userRepository.findById(id).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user);
    }

    private UserPrincipal toPrincipal(User user) {
        List<String> roles = user.getRoles().stream().map(Role::getCode).sorted().toList();
        List<String> permissions = userRepository.findPermissionCodes(user.getId());
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), user.getFullName(),
                user.getInstitutionId(), roles, permissions, "ACTIVE".equals(user.getStatus()), user.getLockedUntil());
    }
}
