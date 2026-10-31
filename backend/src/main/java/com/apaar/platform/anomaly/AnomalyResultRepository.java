package com.apaar.platform.anomaly;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface AnomalyResultRepository extends JpaRepository<AnomalyResult, UUID> {

    @Query("""
            select a from AnomalyResult a
            where (:status is null or a.status = :status)
              and (:institutionId is null or exists (
                    select 1 from Enrolment e where e.id = a.enrolmentId and e.institutionId = :institutionId
              ))
            order by a.createdAt desc
            """)
    Page<AnomalyResult> search(@Param("institutionId") UUID institutionId,
                               @Param("status") String status,
                               Pageable pageable);

    long countByStatusIn(Collection<String> statuses);

    @Query("""
            select count(a) from AnomalyResult a
            where a.status in :statuses
              and exists (
                    select 1 from Enrolment e where e.id = a.enrolmentId and e.institutionId = :institutionId
              )
            """)
    long countByStatusInAndInstitutionId(@Param("statuses") Collection<String> statuses,
                                         @Param("institutionId") UUID institutionId);
}

