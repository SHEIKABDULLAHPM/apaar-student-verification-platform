package com.apaar.platform.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CaseEvidenceRepository extends JpaRepository<CaseEvidence, UUID> {
    List<CaseEvidence> findByCaseIdOrderByCollectedAtDesc(UUID caseId);
}

