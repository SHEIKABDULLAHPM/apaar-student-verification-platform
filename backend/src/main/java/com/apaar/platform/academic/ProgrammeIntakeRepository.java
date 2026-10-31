package com.apaar.platform.academic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProgrammeIntakeRepository extends JpaRepository<ProgrammeIntake, UUID> {
    Optional<ProgrammeIntake> findByProgrammeIdAndAcademicYearId(UUID programmeId, UUID academicYearId);

    List<ProgrammeIntake> findByProgrammeIdOrderByAcademicYearId(UUID programmeId);

    List<ProgrammeIntake> findByAcademicYearId(UUID academicYearId);

    @Query("""
            select pi from ProgrammeIntake pi, Programme p
            where pi.programmeId = p.id and p.institutionId = :institutionId
              and pi.academicYearId = :academicYearId
            """)
    List<ProgrammeIntake> findForInstitutionAndYear(@Param("institutionId") UUID institutionId,
                                                    @Param("academicYearId") UUID academicYearId);

    @Query("""
            select pi from ProgrammeIntake pi, Programme p
            where pi.programmeId = p.id and p.institutionId = :institutionId
            """)
    List<ProgrammeIntake> findForInstitution(@Param("institutionId") UUID institutionId);
}
