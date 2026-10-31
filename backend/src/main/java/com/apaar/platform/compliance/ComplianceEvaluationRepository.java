package com.apaar.platform.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ComplianceEvaluationRepository extends JpaRepository<ComplianceEvaluation, UUID> {
    List<ComplianceEvaluation> findByInstitutionIdOrderByEvaluatedAtDesc(UUID institutionId);
}

