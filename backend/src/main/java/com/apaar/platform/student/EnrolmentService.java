package com.apaar.platform.student;

import com.apaar.platform.academic.AcademicYear;
import com.apaar.platform.academic.AcademicYearRepository;
import com.apaar.platform.academic.Programme;
import com.apaar.platform.academic.ProgrammeRepository;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.institution.Institution;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.student.dto.StudentDtos.EnrolmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EnrolmentService {

    private final EnrolmentRepository enrolmentRepository;
    private final StudentRepository studentRepository;
    private final InstitutionRepository institutionRepository;
    private final ProgrammeRepository programmeRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AccessScopeService accessScope;

    public EnrolmentService(EnrolmentRepository enrolmentRepository,
                            StudentRepository studentRepository,
                            InstitutionRepository institutionRepository,
                            ProgrammeRepository programmeRepository,
                            AcademicYearRepository academicYearRepository,
                            AccessScopeService accessScope) {
        this.enrolmentRepository = enrolmentRepository;
        this.studentRepository = studentRepository;
        this.institutionRepository = institutionRepository;
        this.programmeRepository = programmeRepository;
        this.academicYearRepository = academicYearRepository;
        this.accessScope = accessScope;
    }

    @Transactional(readOnly = true)
    public PageResponse<EnrolmentDto> listEnrolments(int page, int size, UUID requestedInstitutionId,
                                                      UUID academicYearId, UUID programmeId, String status) {
        UUID effectiveInstitutionId = accessScope.resolveInstitutionFilter(requestedInstitutionId);
        Pageable pageable = PageRequest.of(page, size);
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        Page<Enrolment> enrolmentPage = enrolmentRepository.search(effectiveInstitutionId, academicYearId, programmeId, cleanStatus, pageable);
        return PageResponse.from(enrolmentPage, this::toEnrolmentDto);
    }

    private EnrolmentDto toEnrolmentDto(Enrolment e) {
        String studentName = studentRepository.findById(e.getStudentId())
                .map(Student::getFullName).orElse(null);
        String instName = institutionRepository.findById(e.getInstitutionId())
                .map(Institution::getName).orElse(null);
        String progName = programmeRepository.findById(e.getProgrammeId())
                .map(Programme::getName).orElse(null);
        String ayCode = academicYearRepository.findById(e.getAcademicYearId())
                .map(AcademicYear::getCode).orElse(null);

        return new EnrolmentDto(
                e.getId(),
                e.getStudentId(),
                studentName,
                e.getInstitutionId(),
                instName,
                e.getProgrammeId(),
                progName,
                ayCode,
                e.getAcademicYearId(),
                e.getEnrolmentNumber(),
                e.getAdmissionCategory(),
                e.getAdmissionDate(),
                e.getStatus()
        );
    }
}

