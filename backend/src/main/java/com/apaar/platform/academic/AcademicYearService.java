package com.apaar.platform.academic;

import com.apaar.platform.audit.AuditService;
import com.apaar.platform.common.exception.ConflictException;
import com.apaar.platform.common.exception.UnprocessableEntityException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AcademicYearService {
    private final AcademicYearRepository repository;
    private final AuditService auditService;

    public AcademicYearService(AcademicYearRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AcademicDtos.AcademicYearDto> list() {
        return repository.findAllByOrderByStartDateDesc().stream().map(AcademicDtos.AcademicYearDto::from).toList();
    }

    @Transactional
    public AcademicDtos.AcademicYearDto create(AcademicDtos.CreateAcademicYearRequest request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new UnprocessableEntityException("INVALID_ACADEMIC_YEAR", "endDate must be after startDate",
                    Map.of("startDate", request.startDate(), "endDate", request.endDate()));
        }
        if (repository.existsByCode(request.code())) {
            throw new ConflictException("ACADEMIC_YEAR_EXISTS", "Academic year code already exists");
        }
        if (request.active()) {
            repository.findByActiveTrue().forEach(existing -> {
                existing.setActive(false);
                repository.save(existing);
            });
        }
        AcademicYear year = new AcademicYear();
        year.setCode(request.code());
        year.setStartDate(request.startDate());
        year.setEndDate(request.endDate());
        year.setActive(request.active());
        year = repository.save(year);
        auditService.record("ACADEMIC_YEAR_CREATED", "ACADEMIC_YEAR", year.getId().toString(), "CREATE",
                "SUCCESS", null, Map.of("code", year.getCode()));
        return AcademicDtos.AcademicYearDto.from(year);
    }
}
