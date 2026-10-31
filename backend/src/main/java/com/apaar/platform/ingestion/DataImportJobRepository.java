package com.apaar.platform.ingestion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataImportJobRepository extends JpaRepository<DataImportJob, UUID>, JpaSpecificationExecutor<DataImportJob> {
    Optional<DataImportJob> findByIdempotencyKey(String idempotencyKey);

    List<DataImportJob> findByInstitutionIdOrderByCreatedAtDesc(UUID institutionId);

    List<DataImportJob> findByParentImportId(UUID parentImportId);

    @Query("""
            SELECT j FROM DataImportJob j
            WHERE (:institutionId IS NULL OR j.institutionId = :institutionId)
              AND (:academicYearId IS NULL OR j.academicYearId = :academicYearId)
              AND (:status IS NULL OR j.status = cast(:status as string))
            ORDER BY j.createdAt DESC
            """)
    Page<DataImportJob> search(
            @Param("institutionId") UUID institutionId,
            @Param("academicYearId") UUID academicYearId,
            @Param("status") String status,
            Pageable pageable
    );

    @Query("""
            SELECT j FROM DataImportJob j
            WHERE j.institutionId = :institutionId
              AND j.academicYearId = :academicYearId
              AND (:programmeId IS NULL OR j.programmeId = :programmeId)
              AND j.status = 'COMPLETED'
            ORDER BY j.completedAt DESC
            """)
    List<DataImportJob> findCompletedPriorJobs(
            @Param("institutionId") UUID institutionId,
            @Param("academicYearId") UUID academicYearId,
            @Param("programmeId") UUID programmeId
    );
}

