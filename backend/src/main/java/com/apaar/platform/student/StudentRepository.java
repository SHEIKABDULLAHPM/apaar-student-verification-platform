package com.apaar.platform.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByApaarId(String apaarId);
    Optional<Student> findFirstByFullNameIgnoreCaseAndDateOfBirth(String fullName, LocalDate dateOfBirth);
    boolean existsByApaarIdAndIdNot(String apaarId, UUID id);

    @Query("""
            select s from Student s
            where (:search is null or lower(s.fullName) like concat('%', lower(cast(:search as string)), '%')
                   or s.apaarId like concat('%', cast(:search as string), '%'))
            """)
    Page<Student> searchAll(@Param("search") String search, Pageable pageable);

    @Query("""
            select s from Student s
            where exists (select 1 from Enrolment e where e.studentId = s.id and e.institutionId = :institutionId)
              and (:search is null or lower(s.fullName) like concat('%', lower(cast(:search as string)), '%')
                   or s.apaarId like concat('%', cast(:search as string), '%'))
            """)
    Page<Student> searchForInstitution(@Param("institutionId") UUID institutionId, @Param("search") String search,
                                       Pageable pageable);

    long countByApaarFormatValidTrue();

    @Query("""
            select count(distinct s.id) from Student s
            where s.apaarFormatValid = true
              and (:institutionId is null or exists (
                    select 1 from Enrolment e where e.studentId = s.id and e.institutionId = :institutionId))
            """)
    long countApaarFormatValid(@Param("institutionId") UUID institutionId);

    @Query("""
            select count(distinct s.id) from Student s, Enrolment e
            where e.studentId = s.id and e.institutionId = :institutionId
            """)
    long countDistinctByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("""
            select count(distinct s.id) from Student s
            where exists (
                select 1 from ApaarVerificationRecord r
                where r.studentId = s.id and r.status = 'VERIFIED'
            )
            """)
    long countApaarVerified();

    @Query("""
            select count(distinct s.id) from Student s
            where exists (select 1 from Enrolment e where e.studentId = s.id and e.institutionId = :institutionId)
              and exists (select 1 from ApaarVerificationRecord r where r.studentId = s.id and r.status = 'VERIFIED')
            """)
    long countApaarVerifiedByInstitutionId(@Param("institutionId") UUID institutionId);
}
