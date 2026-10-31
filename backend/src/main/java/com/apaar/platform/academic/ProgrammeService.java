package com.apaar.platform.academic;

import com.apaar.platform.audit.AuditService;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.common.exception.ConflictException;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.institution.InstitutionService;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProgrammeService {
    private final ProgrammeRepository programmeRepository;
    private final ProgrammeIntakeRepository intakeRepository;
    private final ProgrammeIntakeCategoryRepository categoryRepository;
    private final AcademicYearRepository academicYearRepository;
    private final InstitutionService institutionService;
    private final AccessScopeService accessScope;
    private final AuditService auditService;

    public ProgrammeService(ProgrammeRepository programmeRepository, ProgrammeIntakeRepository intakeRepository,
                            ProgrammeIntakeCategoryRepository categoryRepository, AcademicYearRepository academicYearRepository,
                            InstitutionService institutionService, AccessScopeService accessScope, AuditService auditService) {
        this.programmeRepository = programmeRepository;
        this.intakeRepository = intakeRepository;
        this.categoryRepository = categoryRepository;
        this.academicYearRepository = academicYearRepository;
        this.institutionService = institutionService;
        this.accessScope = accessScope;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AcademicDtos.ProgrammeDto> list(UUID institutionId, String search, int page, int size) {
        UUID filter = accessScope.resolveInstitutionFilter(institutionId);
        Page<Programme> result = programmeRepository.search(filter, blankToNull(search),
                PageRequest.of(page, size, Sort.by("name")));
        AcademicYear activeYear = academicYearRepository.findFirstByActiveTrue().orElse(null);
        return PageResponse.from(result, p -> toDto(p, activeYear));
    }

    @Transactional(readOnly = true)
    public AcademicDtos.ProgrammeDto get(UUID id) {
        Programme programme = require(id);
        accessScope.assertInstitutionAccess(programme.getInstitutionId());
        return toDto(programme, academicYearRepository.findFirstByActiveTrue().orElse(null));
    }

    public Programme require(UUID id) {
        return programmeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Programme", id));
    }

    @Transactional
    public AcademicDtos.ProgrammeDto create(AcademicDtos.CreateProgrammeRequest request, UserPrincipal actor) {
        UUID institutionId = accessScope.requireWritableInstitution(request.institutionId());
        institutionService.require(institutionId);
        if (programmeRepository.findByInstitutionIdAndCodeIgnoreCase(institutionId, request.code().trim()).isPresent()) {
            throw new ConflictException("PROGRAMME_CODE_EXISTS", "Programme code already exists for this institution");
        }
        Programme programme = new Programme();
        programme.setInstitutionId(institutionId);
        programme.setCode(request.code().trim().toUpperCase());
        programme.setName(request.name().trim());
        programme.setDegreeLevel(request.degreeLevel());
        programme.setDurationYears(request.durationYears());
        programme.setCreatedBy(actor.getUserId());
        programme = programmeRepository.save(programme);
        auditService.record("PROGRAMME_CREATED", "PROGRAMME", programme.getId().toString(), "CREATE", "SUCCESS",
                institutionId, Map.of("code", programme.getCode()));
        return toDto(programme, academicYearRepository.findFirstByActiveTrue().orElse(null));
    }

    @Transactional
    public AcademicDtos.ProgrammeDto update(UUID id, AcademicDtos.UpdateProgrammeRequest request) {
        Programme programme = require(id);
        accessScope.assertInstitutionAccess(programme.getInstitutionId());
        if (request.name() != null) programme.setName(request.name().trim());
        if (request.degreeLevel() != null) programme.setDegreeLevel(request.degreeLevel());
        if (request.durationYears() != null) programme.setDurationYears(request.durationYears());
        if (request.status() != null) programme.setStatus(request.status());
        programme = programmeRepository.save(programme);
        auditService.record("PROGRAMME_UPDATED", "PROGRAMME", id.toString(), "UPDATE", "SUCCESS",
                programme.getInstitutionId(), Map.of());
        return toDto(programme, academicYearRepository.findFirstByActiveTrue().orElse(null));
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.IntakeDto> listIntakes(UUID programmeId) {
        Programme programme = require(programmeId);
        accessScope.assertInstitutionAccess(programme.getInstitutionId());
        return intakeRepository.findByProgrammeIdOrderByAcademicYearId(programmeId).stream().map(this::toIntakeDto).toList();
    }

    @Transactional
    public AcademicDtos.IntakeDto upsertIntake(UUID programmeId, UUID academicYearId,
                                               AcademicDtos.UpsertIntakeRequest request, UserPrincipal actor) {
        Programme programme = require(programmeId);
        accessScope.assertInstitutionAccess(programme.getInstitutionId());
        academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", academicYearId));

        List<AcademicDtos.CategorySeat> seats = request.categories() == null ? List.of() : request.categories();
        Set<String> seen = new HashSet<>();
        for (AcademicDtos.CategorySeat seat : seats) {
            if (!seen.add(seat.category())) {
                throw new ConflictException("DUPLICATE_CATEGORY", "Each category may appear only once");
            }
        }

        ProgrammeIntake intake = intakeRepository.findByProgrammeIdAndAcademicYearId(programmeId, academicYearId)
                .orElseGet(ProgrammeIntake::new);
        intake.setProgrammeId(programmeId);
        intake.setAcademicYearId(academicYearId);
        intake.setApprovedIntake(request.approvedIntake());
        intake.setSupernumeraryIntake(request.supernumeraryIntake());
        intake.setNriIntake(request.nriIntake());
        intake.setOciIntake(request.ociIntake());
        intake.setForeignNationalIntake(request.foreignNationalIntake());
        if (intake.getCreatedBy() == null) intake.setCreatedBy(actor.getUserId());
        final ProgrammeIntake saved = intakeRepository.save(intake);

        categoryRepository.deleteByProgrammeIntakeId(saved.getId());
        categoryRepository.flush();
        List<ProgrammeIntakeCategory> entities = new ArrayList<>();
        for (AcademicDtos.CategorySeat seat : seats) {
            ProgrammeIntakeCategory category = new ProgrammeIntakeCategory();
            category.setProgrammeIntakeId(saved.getId());
            category.setCategory(seat.category());
            category.setApprovedSeats(seat.approvedSeats());
            entities.add(category);
        }
        categoryRepository.saveAll(entities);
        auditService.record("PROGRAMME_INTAKE_CONFIGURED", "PROGRAMME_INTAKE", saved.getId().toString(), "UPSERT",
                "SUCCESS", programme.getInstitutionId(), Map.of("academicYearId", academicYearId.toString(),
                        "approvedIntake", request.approvedIntake()));
        return toIntakeDto(saved);
    }

    private AcademicDtos.ProgrammeDto toDto(Programme p, AcademicYear activeYear) {
        Integer approved = null;
        if (activeYear != null) {
            approved = intakeRepository.findByProgrammeIdAndAcademicYearId(p.getId(), activeYear.getId())
                    .map(ProgrammeIntake::getApprovedIntake).orElse(null);
        }
        return new AcademicDtos.ProgrammeDto(p.getId(), p.getInstitutionId(), p.getCode(), p.getName(),
                p.getDegreeLevel(), p.getDurationYears(), p.getStatus(), approved, p.getCreatedAt());
    }

    private AcademicDtos.IntakeDto toIntakeDto(ProgrammeIntake i) {
        List<AcademicDtos.CategorySeat> seats = categoryRepository.findByProgrammeIntakeId(i.getId()).stream()
                .map(c -> new AcademicDtos.CategorySeat(c.getCategory(), c.getApprovedSeats())).toList();
        return new AcademicDtos.IntakeDto(i.getId(), i.getProgrammeId(), i.getAcademicYearId(), i.getApprovedIntake(),
                i.getSupernumeraryIntake(), i.getNriIntake(), i.getOciIntake(), i.getForeignNationalIntake(),
                i.getStatus(), seats);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
