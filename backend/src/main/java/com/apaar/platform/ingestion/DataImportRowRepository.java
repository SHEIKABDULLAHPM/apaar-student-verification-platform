package com.apaar.platform.ingestion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataImportRowRepository extends JpaRepository<DataImportRow, UUID> {
    List<DataImportRow> findByImportJobIdOrderByRowNumberAsc(UUID importJobId);

    Page<DataImportRow> findByImportJobIdOrderByRowNumberAsc(UUID importJobId, Pageable pageable);

    Page<DataImportRow> findByImportJobIdAndRowStatusOrderByRowNumberAsc(UUID importJobId, String rowStatus, Pageable pageable);

    Optional<DataImportRow> findByImportJobIdAndRowNumber(UUID importJobId, int rowNumber);

    long countByImportJobIdAndRowStatus(UUID importJobId, String rowStatus);

    @Modifying
    @Query("DELETE FROM DataImportRow r WHERE r.importJobId = :importJobId")
    void deleteByImportJobId(@Param("importJobId") UUID importJobId);
}

