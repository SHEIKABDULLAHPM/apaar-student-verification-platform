package com.apaar.platform.apaar;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApaarVerificationRecordRepository extends JpaRepository<ApaarVerificationRecord, UUID> {
    List<ApaarVerificationRecord> findByStudentIdOrderByRequestedAtDesc(UUID studentId);
    Optional<ApaarVerificationRecord> findFirstByStudentIdOrderByRequestedAtDesc(UUID studentId);
    Optional<ApaarVerificationRecord> findFirstByStudentIdAndStatusOrderByRequestedAtDesc(UUID studentId, String status);

    @Query("""
            select count(r) from ApaarVerificationRecord r
            where r.status = :status and r.verificationMode = :verificationMode
              and (:institutionId is null or exists (
                    select 1 from Enrolment e where e.studentId = r.studentId and e.institutionId = :institutionId))
            """)
    long countByStatusAndVerificationModeAndInstitutionId(@Param("status") String status,
                                                          @Param("verificationMode") String verificationMode,
                                                          @Param("institutionId") UUID institutionId);

    @Query("""
            select count(r) from ApaarVerificationRecord r
            where r.status = :status
              and (:institutionId is null or exists (
                    select 1 from Enrolment e where e.studentId = r.studentId and e.institutionId = :institutionId))
            """)
    long countByStatusAndInstitutionId(@Param("status") String status,
                                       @Param("institutionId") UUID institutionId);
}
