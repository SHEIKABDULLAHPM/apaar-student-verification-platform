package com.apaar.platform.identity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IdentityMatchingRunRepository extends JpaRepository<IdentityMatchingRun, UUID> {
    Page<IdentityMatchingRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}

