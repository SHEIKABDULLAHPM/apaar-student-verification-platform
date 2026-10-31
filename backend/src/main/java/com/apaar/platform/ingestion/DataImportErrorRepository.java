package com.apaar.platform.ingestion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface DataImportErrorRepository extends JpaRepository<DataImportError, UUID> {
    List<DataImportError> findByImportJobIdOrderByRowNumberAsc(UUID importJobId);

    Page<DataImportError> findByImportJobIdOrderByRowNumberAsc(UUID importJobId, Pageable pageable);

    List<DataImportError> findByImportJobIdAndRowNumber(UUID importJobId, int rowNumber);

    long countByImportJobId(UUID importJobId);

    @Modifying
    @Query("DELETE FROM DataImportError e WHERE e.importJobId = :importJobId")
    void deleteByImportJobId(@Param("importJobId") UUID importJobId);
}

