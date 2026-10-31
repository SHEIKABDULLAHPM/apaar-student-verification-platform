package com.apaar.platform.identity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IdentityMatchRepository extends JpaRepository<IdentityMatch, UUID> {
    Page<IdentityMatch> findByRunIdOrderByCreatedAtDesc(UUID runId, Pageable pageable);

    @Query("""
            select m from IdentityMatch m
            where (:classification is null or m.matchClassification = :classification)
              and (:institutionId is null or exists (
                    select 1 from Enrolment e where e.institutionId = :institutionId
                      and (e.studentId = m.primaryStudentId or e.studentId = m.secondaryStudentId)))
            order by m.createdAt desc
            """)
    Page<IdentityMatch> search(@Param("classification") String classification,
                               @Param("institutionId") UUID institutionId,
                               Pageable pageable);

    @Query("""
            select m from IdentityMatch m
            where m.primaryStudentId = :studentId or m.secondaryStudentId = :studentId
            order by m.createdAt desc
            """)
    List<IdentityMatch> findByStudentId(@Param("studentId") UUID studentId);

    @Query("""
            select count(distinct m.conflictId) from IdentityMatch m
            where m.matchClassification = :matchClassification
              and m.conflictId is not null
              and (:institutionId is null or exists (
                    select 1 from Enrolment e where e.institutionId = :institutionId
                      and (e.studentId = m.primaryStudentId or e.studentId = m.secondaryStudentId)))
            """)
    long countDistinctConflictsByMatchClassification(@Param("matchClassification") String matchClassification,
                                                     @Param("institutionId") UUID institutionId);
}

