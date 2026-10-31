package com.apaar.platform.student;

import com.apaar.platform.academic.AcademicYear;
import com.apaar.platform.academic.AcademicYearRepository;
import com.apaar.platform.academic.Programme;
import com.apaar.platform.academic.ProgrammeRepository;
import com.apaar.platform.apaar.ApaarVerificationRecord;
import com.apaar.platform.apaar.ApaarVerificationRecordRepository;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.institution.Institution;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.student.dto.StudentDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final ApaarVerificationRecordRepository verificationRepository;
    private final InstitutionRepository institutionRepository;
    private final ProgrammeRepository programmeRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AccessScopeService accessScope;

    public StudentService(StudentRepository studentRepository,
                          EnrolmentRepository enrolmentRepository,
                          ApaarVerificationRecordRepository verificationRepository,
                          InstitutionRepository institutionRepository,
                          ProgrammeRepository programmeRepository,
                          AcademicYearRepository academicYearRepository,
                          AccessScopeService accessScope) {
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.verificationRepository = verificationRepository;
        this.institutionRepository = institutionRepository;
        this.programmeRepository = programmeRepository;
        this.academicYearRepository = academicYearRepository;
        this.accessScope = accessScope;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentSummaryDto> listStudents(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Student> studentPage;
        if (accessScope.isPlatformWide()) {
            studentPage = studentRepository.searchAll(cleanSearch, pageable);
        } else {
            UUID institutionId = accessScope.requirePrincipal().getInstitutionId();
            studentPage = studentRepository.searchForInstitution(institutionId, cleanSearch, pageable);
        }

        return PageResponse.from(studentPage, this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public StudentDetailDto getStudent(UUID id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("STUDENT_NOT_FOUND", "Student not found: " + id));

        if (!accessScope.isPlatformWide()) {
            UUID institutionId = accessScope.requirePrincipal().getInstitutionId();
            boolean hasEnrolment = enrolmentRepository.existsByStudentIdAndInstitutionId(student.getId(), institutionId);
            if (!hasEnrolment) {
                throw new ResourceNotFoundException("STUDENT_NOT_FOUND", "Student not found: " + id);
            }
        }

        List<Enrolment> enrolments = enrolmentRepository.findByStudentIdOrderByCreatedAtDesc(student.getId());
        List<StudentEnrolmentDto> enrolmentDtos = enrolments.stream()
                .map(this::toStudentEnrolmentDto)
                .toList();

        String verifStatus = resolveVerificationStatus(student);

        return new StudentDetailDto(
                student.getId(),
                student.getApaarId(),
                student.getFullName(),
                student.getDateOfBirth(),
                student.getGender(),
                student.getCategory(),
                student.isApaarFormatValid(),
                verifStatus,
                student.getCreatedAt(),
                enrolmentDtos
        );
    }

    private StudentSummaryDto toSummaryDto(Student s) {
        String verifStatus = resolveVerificationStatus(s);
        return new StudentSummaryDto(
                s.getId(),
                s.getApaarId(),
                s.getFullName(),
                s.getDateOfBirth(),
                s.getGender(),
                s.getCategory(),
                s.isApaarFormatValid(),
                verifStatus,
                s.getCreatedAt()
        );
    }

    private StudentEnrolmentDto toStudentEnrolmentDto(Enrolment e) {
        String instName = institutionRepository.findById(e.getInstitutionId())
                .map(Institution::getName).orElse(null);
        String progName = programmeRepository.findById(e.getProgrammeId())
                .map(Programme::getName).orElse(null);
        String ayCode = academicYearRepository.findById(e.getAcademicYearId())
                .map(AcademicYear::getCode).orElse(null);

        return new StudentEnrolmentDto(
                e.getId(),
                e.getInstitutionId(),
                instName,
                progName,
                ayCode,
                e.getEnrolmentNumber(),
                e.getAdmissionCategory(),
                e.getStatus()
        );
    }

    private String resolveVerificationStatus(Student s) {
        return verificationRepository.findFirstByStudentIdOrderByRequestedAtDesc(s.getId())
                .map(ApaarVerificationRecord::getStatus)
                .orElse(s.getApaarId() != null && !s.getApaarId().isBlank() ? "NOT_VERIFIED" : "PENDING");
    }
}

