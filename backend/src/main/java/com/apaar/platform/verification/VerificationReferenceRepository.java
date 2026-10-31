package com.apaar.platform.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VerificationReferenceRepository extends JpaRepository<VerificationReference, UUID> {
    Optional<VerificationReference> findByPublicRef(String publicRef);
}

