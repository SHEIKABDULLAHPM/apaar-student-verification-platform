package com.apaar.platform.identity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityConflictRepository extends JpaRepository<IdentityConflict, UUID> {
    @Query("""
            select c from IdentityConflict c
            where (:status is null or c.status = :status)
              and (:institutionId is null or exists (
                    select 1 from Enrolment e where e.institutionId = :institutionId
                      and (e.id = c.primaryEnrolmentId or e.id = c.secondaryEnrolmentId)))
            order by c.createdAt desc
            """)
    Page<IdentityConflict> search(@Param("institutionId") UUID institutionId, @Param("status") String status,
                                  Pageable pageable);

    @Query("""
            select c from IdentityConflict c
            where c.status in ('OPEN','INVESTIGATING') and c.reasonCode = :reason
              and ((c.primaryStudentId = :a and c.secondaryStudentId = :b)
                or (c.primaryStudentId = :b and c.secondaryStudentId = :a))
            """)
    Optional<IdentityConflict> findOpenByStudents(@Param("reason") String reason, @Param("a") UUID a, @Param("b") UUID b);

    @Query("""
            select c from IdentityConflict c
            where c.status in ('OPEN','INVESTIGATING') and c.reasonCode = :reason
              and ((c.primaryEnrolmentId = :a and c.secondaryEnrolmentId = :b)
                or (c.primaryEnrolmentId = :b and c.secondaryEnrolmentId = :a))
            """)
    Optional<IdentityConflict> findOpenByEnrolments(@Param("reason") String reason, @Param("a") UUID a, @Param("b") UUID b);

    Optional<IdentityConflict> findByFingerprint(String fingerprint);

    Optional<IdentityConflict> findFirstByFingerprintAndStatusIn(String fingerprint, java.util.Collection<String> statuses);

    List<IdentityConflict> findByPrimaryStudentIdOrSecondaryStudentId(UUID primaryStudentId, UUID secondaryStudentId);

    long countByStatus(String status);

    long countByStatusIn(java.util.Collection<String> statuses);

    @Query("""
            select count(c) from IdentityConflict c
            where c.status in :statuses
              and (:institutionId is null or exists (
                    select 1 from Enrolment e where e.institutionId = :institutionId
                      and (e.id = c.primaryEnrolmentId or e.id = c.secondaryEnrolmentId)))
            """)
    long countByStatusInAndInstitutionId(@Param("statuses") java.util.Collection<String> statuses,
                                         @Param("institutionId") UUID institutionId);
}
