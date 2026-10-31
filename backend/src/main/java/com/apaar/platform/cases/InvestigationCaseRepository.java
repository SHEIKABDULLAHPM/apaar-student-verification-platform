package com.apaar.platform.cases;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvestigationCaseRepository extends JpaRepository<InvestigationCase, UUID> {
    Optional<InvestigationCase> findByCaseNumber(String caseNumber);

    @Query("""
            select c from InvestigationCase c
            where (:institutionId is null or c.institutionId = :institutionId or c.institutionId is null)
              and (:status is null or c.status = :status)
            order by c.createdAt desc
            """)
    Page<InvestigationCase> search(@Param("institutionId") UUID institutionId,
                                  @Param("status") String status,
                                  Pageable pageable);

    @Query("""
            select count(c) from InvestigationCase c
            where c.status in :statuses
              and (:institutionId is null or c.institutionId = :institutionId or c.institutionId is null)
            """)
    long countByStatusInAndInstitutionId(@Param("statuses") java.util.Collection<String> statuses,
                                         @Param("institutionId") UUID institutionId);
}

