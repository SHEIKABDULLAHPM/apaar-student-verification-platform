package com.apaar.platform.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CaseDecisionRepository extends JpaRepository<CaseDecision, UUID> {
    List<CaseDecision> findByCaseIdOrderByDecidedAtDesc(UUID caseId);
}

