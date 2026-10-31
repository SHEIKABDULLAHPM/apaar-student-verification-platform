package com.apaar.platform.academic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {
    Optional<AcademicYear> findByCode(String code);
    boolean existsByCode(String code);
    List<AcademicYear> findAllByOrderByStartDateDesc();
    List<AcademicYear> findByActiveTrue();
    Optional<AcademicYear> findFirstByActiveTrue();
}
