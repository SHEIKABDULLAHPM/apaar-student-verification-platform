package com.apaar.platform.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VerificationEventRepository extends JpaRepository<VerificationEvent, UUID> {
}

