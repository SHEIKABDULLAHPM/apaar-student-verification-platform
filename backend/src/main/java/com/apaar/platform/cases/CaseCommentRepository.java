package com.apaar.platform.cases;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CaseCommentRepository extends JpaRepository<CaseComment, UUID> {
    List<CaseComment> findByCaseIdOrderByCreatedAtAsc(UUID caseId);
}

