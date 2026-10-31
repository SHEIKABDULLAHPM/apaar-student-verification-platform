package com.apaar.platform.compliance;

import com.apaar.platform.academic.Programme;
import com.apaar.platform.academic.ProgrammeIntake;
import com.apaar.platform.academic.ProgrammeIntakeRepository;
import com.apaar.platform.academic.ProgrammeRepository;
import com.apaar.platform.audit.AuditService;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.compliance.ComplianceDtos.ComplianceEvaluationResultDto;
import com.apaar.platform.compliance.ComplianceDtos.ComplianceViolationDto;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ComplianceService {

    private final ComplianceViolationRepository violationRepository;
    private final ComplianceEvaluationRepository evaluationRepository;
    private final ProgrammeIntakeRepository intakeRepository;
    private final ProgrammeRepository programmeRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final StudentRepository studentRepository;
    private final AccessScopeService accessScope;
    private final AuditService auditService;

    public ComplianceService(ComplianceViolationRepository violationRepository,
                             ComplianceEvaluationRepository evaluationRepository,
                             ProgrammeIntakeRepository intakeRepository,
                             ProgrammeRepository programmeRepository,
                             EnrolmentRepository enrolmentRepository,
                             StudentRepository studentRepository,
                             AccessScopeService accessScope,
                             AuditService auditService) {
        this.violationRepository = violationRepository;
        this.evaluationRepository = evaluationRepository;
        this.intakeRepository = intakeRepository;
        this.programmeRepository = programmeRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.studentRepository = studentRepository;
        this.accessScope = accessScope;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ComplianceViolationDto> listViolations(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        UUID institutionFilter = null;
        if (!accessScope.isPlatformWide()) {
            institutionFilter = accessScope.requirePrincipal().getInstitutionId();
        }

        Page<ComplianceViolation> violationPage = violationRepository.search(institutionFilter, cleanStatus, pageable);
        return PageResponse.from(violationPage, this::toDto);
    }

    @Transactional
    public ComplianceEvaluationResultDto evaluateCompliance(UUID requestedInstitutionId, UUID academicYearId) {
        UUID effectiveInstitutionId = accessScope.resolveInstitutionFilter(requestedInstitutionId);
        if (effectiveInstitutionId == null && !accessScope.isPlatformWide()) {
            effectiveInstitutionId = accessScope.requirePrincipal().getInstitutionId();
        }

        UUID actorId = accessScope.requirePrincipal().getUserId();
        ComplianceEvaluation evaluation = new ComplianceEvaluation(effectiveInstitutionId, academicYearId, actorId);
        evaluation = evaluationRepository.save(evaluation);

        List<ComplianceViolation> newViolations = new ArrayList<>();

        if (effectiveInstitutionId != null) {
            // Check Programme Intakes
            List<ProgrammeIntake> intakes = intakeRepository.findForInstitutionAndYear(effectiveInstitutionId, academicYearId);
            for (ProgrammeIntake pi : intakes) {
                long activeCount = enrolmentRepository.countActiveForProgrammeYear(pi.getProgrammeId(), academicYearId);
                int totalApproved = pi.getApprovedIntake() + pi.getSupernumeraryIntake();
                if (activeCount > totalApproved) {
                    Programme p = programmeRepository.findById(pi.getProgrammeId()).orElse(null);
                    Map<String, Object> details = new LinkedHashMap<>();
                    details.put("programmeCode", p != null ? p.getCode() : pi.getProgrammeId().toString());
                    details.put("programmeName", p != null ? p.getName() : "Unknown");
                    details.put("approvedIntake", totalApproved);
                    details.put("actualEnrolment", activeCount);
                    newViolations.add(new ComplianceViolation(
                            evaluation.getId(), null, null, null,
                            "INTAKE_EXCESS", "HIGH", details
                    ));
                }
            }

            // Check enrolments for this institution & academic year
            List<Enrolment> enrolments = enrolmentRepository.findByInstitutionIdAndAcademicYearId(effectiveInstitutionId, academicYearId);
            for (Enrolment e : enrolments) {
                if (e.getStudentId() != null) {
                    Optional<Student> studentOpt = studentRepository.findById(e.getStudentId());
                    if (studentOpt.isPresent()) {
                        Student s = studentOpt.get();
                        if (!s.isApaarFormatValid()) {
                            Map<String, Object> details = new LinkedHashMap<>();
                            details.put("apaarId", s.getApaarId());
                            details.put("studentName", s.getFullName());
                            newViolations.add(new ComplianceViolation(
                                    evaluation.getId(), null, e.getId(), s.getId(),
                                    "APAAR_FORMAT_INVALID", "MEDIUM", details
                            ));
                        }
                    }
                }
            }
        }

        if (!newViolations.isEmpty()) {
            violationRepository.saveAll(newViolations);
        }

        evaluation.setTotalFindings(newViolations.size());
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("findingsCount", newViolations.size());
        summary.put("evaluatedAt", evaluation.getEvaluatedAt().toString());
        evaluation.setSummary(summary);
        evaluationRepository.save(evaluation);

        auditService.record("COMPLIANCE_EVALUATED", "COMPLIANCE_EVALUATION",
                evaluation.getId().toString(), "EVALUATE", "SUCCESS", effectiveInstitutionId, summary);

        return new ComplianceEvaluationResultDto(
                evaluation.getId(),
                evaluation.getInstitutionId(),
                evaluation.getAcademicYearId(),
                evaluation.getStatus(),
                evaluation.getTotalFindings(),
                evaluation.getSummary()
        );
    }

    private ComplianceViolationDto toDto(ComplianceViolation v) {
        return new ComplianceViolationDto(
                v.getId() != null ? v.getId().toString() : null,
                v.getReasonCode(),
                v.getSeverity(),
                v.getStatus(),
                v.getDetails(),
                v.getCreatedAt()
        );
    }
}
