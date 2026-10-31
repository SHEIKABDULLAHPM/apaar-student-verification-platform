package com.apaar.platform.academic;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgrammeRepository extends JpaRepository<Programme, UUID> {
    Optional<Programme> findByInstitutionIdAndCodeIgnoreCase(UUID institutionId, String code);

    List<Programme> findByInstitutionIdOrderByName(UUID institutionId);

    @Query("""
            select p from Programme p
            where (:institutionId is null or p.institutionId = :institutionId)
              and (:search is null or lower(p.name) like concat('%', lower(cast(:search as string)), '%')
                   or lower(p.code) like concat('%', lower(cast(:search as string)), '%'))
            """)
    Page<Programme> search(@Param("institutionId") UUID institutionId, @Param("search") String search, Pageable pageable);

    long countByInstitutionId(UUID institutionId);
}
