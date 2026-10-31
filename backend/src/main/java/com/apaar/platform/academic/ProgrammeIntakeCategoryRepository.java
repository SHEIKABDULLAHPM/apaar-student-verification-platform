package com.apaar.platform.academic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgrammeIntakeCategoryRepository extends JpaRepository<ProgrammeIntakeCategory, UUID> {
    List<ProgrammeIntakeCategory> findByProgrammeIntakeId(UUID programmeIntakeId);
    Optional<ProgrammeIntakeCategory> findByProgrammeIntakeIdAndCategory(UUID programmeIntakeId, String category);
    void deleteByProgrammeIntakeId(UUID programmeIntakeId);
}
