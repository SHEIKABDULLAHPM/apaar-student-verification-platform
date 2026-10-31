package com.apaar.platform.anomaly;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnomalyRunRepository extends JpaRepository<AnomalyRun, UUID> {
}

