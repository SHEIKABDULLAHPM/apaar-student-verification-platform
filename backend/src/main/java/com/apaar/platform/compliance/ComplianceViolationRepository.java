package com.apaar.platform.compliance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface ComplianceViolationRepository extends JpaRepository<ComplianceViolation, UUID> {

    @Query("""
            select v from ComplianceViolation v
            where (:status is null or v.status = :status)
              and (:institutionId is null or exists (
                    select 1 from Enrolment e where e.id = v.enrolmentId and e.institutionId = :institutionId
              ))
            order by v.createdAt desc
            """)
    Page<ComplianceViolation> search(@Param("institutionId") UUID institutionId,
                                     @Param("status") String status,
                                     Pageable pageable);

    long countByStatusIn(Collection<String> statuses);

    @Query("""
            select count(v) from ComplianceViolation v
            where v.status in :statuses
              and exists (
                    select 1 from Enrolment e where e.id = v.enrolmentId and e.institutionId = :institutionId
              )
            """)
    long countByStatusInAndInstitutionId(@Param("statuses") Collection<String> statuses,
                                         @Param("institutionId") UUID institutionId);
}

