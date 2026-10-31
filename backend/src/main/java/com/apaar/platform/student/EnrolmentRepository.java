package com.apaar.platform.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnrolmentRepository extends JpaRepository<Enrolment, UUID> {
    List<Enrolment> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<Enrolment> findByStudentIdAndAcademicYearIdAndStatusIn(UUID studentId, UUID academicYearId, List<String> statuses);
    List<Enrolment> findByInstitutionIdAndAcademicYearIdAndStatusIn(UUID institutionId, UUID academicYearId, List<String> statuses);
    List<Enrolment> findByInstitutionIdAndAcademicYearId(UUID institutionId, UUID academicYearId);
    Optional<Enrolment> findByInstitutionIdAndEnrolmentNumber(UUID institutionId, String enrolmentNumber);
    boolean existsByStudentIdAndInstitutionId(UUID studentId, UUID institutionId);
    boolean existsByStudentIdAndProgrammeIdAndAcademicYearIdAndStatusIn(UUID studentId, UUID programmeId,
                                                                        UUID academicYearId, List<String> statuses);
    long countByInstitutionId(UUID institutionId);
    long countByInstitutionIdAndStatus(UUID institutionId, String status);

    @Query("""
            select e from Enrolment e
            where (:institutionId is null or e.institutionId = :institutionId)
              and (:academicYearId is null or e.academicYearId = :academicYearId)
              and (:programmeId is null or e.programmeId = :programmeId)
              and (:status is null or e.status = :status)
            """)
    Page<Enrolment> search(@Param("institutionId") UUID institutionId, @Param("academicYearId") UUID academicYearId,
                           @Param("programmeId") UUID programmeId, @Param("status") String status, Pageable pageable);

    @Query("""
            select count(e) from Enrolment e
            where e.institutionId = :institutionId and e.academicYearId = :academicYearId
              and e.status in ('PROVISIONAL','ACTIVE')
            """)
    long countActiveForInstitutionYear(@Param("institutionId") UUID institutionId, @Param("academicYearId") UUID academicYearId);

    @Query("""
            select count(e) from Enrolment e
            where e.programmeId = :programmeId and e.academicYearId = :academicYearId
              and e.status in ('PROVISIONAL','ACTIVE')
            """)
    long countActiveForProgrammeYear(@Param("programmeId") UUID programmeId, @Param("academicYearId") UUID academicYearId);

    @Query("""
            select count(e) from Enrolment e
            where e.programmeId = :programmeId and e.academicYearId = :academicYearId
              and e.admissionCategory = :category and e.status in ('PROVISIONAL','ACTIVE')
            """)
    long countActiveForProgrammeYearCategory(@Param("programmeId") UUID programmeId,
                                             @Param("academicYearId") UUID academicYearId,
                                             @Param("category") String category);

    @Query("""
            select count(e) from Enrolment e
            where (:institutionId is null or e.institutionId = :institutionId)
              and e.status in ('PROVISIONAL','ACTIVE')
            """)
    long countActive(@Param("institutionId") UUID institutionId);

    @Query("""
            select e.admissionCategory, count(e) from Enrolment e
            where (:institutionId is null or e.institutionId = :institutionId)
              and e.status in ('PROVISIONAL','ACTIVE')
            group by e.admissionCategory
            """)
    List<Object[]> countActiveByCategory(@Param("institutionId") UUID institutionId);
}
