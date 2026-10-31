package com.apaar.platform.identity;

import com.apaar.platform.audit.AuditService;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.common.exception.UnprocessableEntityException;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.UserPrincipal;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdentityConflictService {
    private static final List<String> ACTIVE_STATUSES = List.of("PROVISIONAL", "ACTIVE");

    private final IdentityConflictRepository repository;
    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final AccessScopeService accessScope;
    private final AuditService auditService;
    private final Clock clock;

    public IdentityConflictService(IdentityConflictRepository repository, StudentRepository studentRepository,
                                   EnrolmentRepository enrolmentRepository, AccessScopeService accessScope,
                                   AuditService auditService, Clock clock) {
        this.repository = repository;
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.accessScope = accessScope;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public Optional<IdentityConflict> detectCrossInstitutionDuplicate(Enrolment enrolment) {
        List<Enrolment> active = enrolmentRepository.findByStudentIdAndAcademicYearIdAndStatusIn(
                enrolment.getStudentId(), enrolment.getAcademicYearId(), ACTIVE_STATUSES);
        Optional<Enrolment> other = active.stream()
                .filter(e -> !e.getId().equals(enrolment.getId()))
                .filter(e -> !e.getInstitutionId().equals(enrolment.getInstitutionId()))
                .findFirst();
        if (other.isEmpty()) {
            return Optional.empty();
        }
        Enrolment secondary = other.get();
        if (repository.findOpenByEnrolments("DUPLICATE_ACTIVE_ENROLMENT", enrolment.getId(), secondary.getId()).isPresent()) {
            return Optional.empty();
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("academicYearId", enrolment.getAcademicYearId().toString());
        details.put("institutionIds", List.of(enrolment.getInstitutionId().toString(), secondary.getInstitutionId().toString()));
        details.put("explanation", "Same student identity has active enrolments at more than one institution in the same academic year. This is a review trigger, not a finding of fraud.");
        IdentityConflict conflict = new IdentityConflict();
        conflict.setConflictType("CROSS_INSTITUTION_ACTIVE_ENROLMENT");
        conflict.setPrimaryStudentId(enrolment.getStudentId());
        conflict.setSecondaryStudentId(secondary.getStudentId());
        conflict.setPrimaryEnrolmentId(enrolment.getId());
        conflict.setSecondaryEnrolmentId(secondary.getId());
        conflict.setDetectionSource("RULE");
        conflict.setStatus("OPEN");
        conflict.setSeverity("HIGH");
        conflict.setReasonCode("DUPLICATE_ACTIVE_ENROLMENT");
        conflict.setDetails(details);
        IdentityConflict saved = repository.save(conflict);
        auditService.record("IDENTITY_CONFLICT_DETECTED", "IDENTITY_CONFLICT", saved.getId().toString(), "DETECT", "SUCCESS",
                enrolment.getInstitutionId(), Map.of("reasonCode", saved.getReasonCode()));
        return Optional.of(saved);
    }

    @Transactional
    public Optional<IdentityConflict> detectApaarDuplicate(Student primary, Student secondary, String apaarId) {
        if (repository.findOpenByStudents("APAAR_ALREADY_ASSIGNED", primary.getId(), secondary.getId()).isPresent()) {
            return Optional.empty();
        }
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("apaarId", apaarId);
        details.put("explanation", "The same APAAR identifier was supplied for two different student identities. Format validity does not establish identity ownership.");
        IdentityConflict conflict = new IdentityConflict();
        conflict.setConflictType("APAAR_IDENTITY_CONFLICT");
        conflict.setPrimaryStudentId(primary.getId());
        conflict.setSecondaryStudentId(secondary.getId());
        conflict.setDetectionSource("RULE");
        conflict.setStatus("OPEN");
        conflict.setSeverity("HIGH");
        conflict.setReasonCode("APAAR_ALREADY_ASSIGNED");
        conflict.setDetails(details);
        IdentityConflict saved = repository.save(conflict);
        auditService.record("IDENTITY_CONFLICT_DETECTED", "IDENTITY_CONFLICT", saved.getId().toString(), "DETECT", "SUCCESS",
                null, Map.of("reasonCode", saved.getReasonCode()));
        return Optional.of(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<IdentityDtos.ConflictDto> list(String status, int page, int size) {
        UUID institutionFilter = accessScope.resolveInstitutionFilter(null);
        Page<IdentityConflict> result = repository.search(institutionFilter, blankToNull(status),
                PageRequest.of(page, size));
        return PageResponse.from(result, this::toDto);
    }

    @Transactional(readOnly = true)
    public IdentityDtos.ConflictDto get(UUID id) {
        IdentityConflict conflict = require(id);
        assertAccess(conflict);
        return toDto(conflict);
    }

    @Transactional
    public IdentityDtos.ConflictDto updateStatus(UUID id, IdentityDtos.UpdateConflictRequest request, UserPrincipal actor) {
        IdentityConflict conflict = require(id);
        assertAccess(conflict);
        if (!List.of("OPEN", "INVESTIGATING", "RESOLVED", "DISMISSED").contains(request.status())) {
            throw new UnprocessableEntityException("INVALID_STATUS", "Unsupported conflict status", Map.of());
        }
        if (List.of("RESOLVED", "DISMISSED").contains(request.status())
                && (request.resolutionNotes() == null || request.resolutionNotes().isBlank())) {
            throw new UnprocessableEntityException("RESOLUTION_NOTES_REQUIRED",
                    "Resolution notes are required when resolving or dismissing a conflict", Map.of());
        }
        conflict.setStatus(request.status());
        if (request.resolutionNotes() != null) conflict.setResolutionNotes(request.resolutionNotes());
        if (List.of("RESOLVED", "DISMISSED").contains(request.status())) {
            conflict.setResolvedAt(clock.instant());
            conflict.setResolvedBy(actor.getUserId());
        }
        conflict = repository.save(conflict);
        auditService.record("IDENTITY_CONFLICT_UPDATED", "IDENTITY_CONFLICT", id.toString(), "UPDATE", "SUCCESS",
                null, Map.of("status", request.status()));
        return toDto(conflict);
    }

    public IdentityConflict require(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("IdentityConflict", id));
    }

    public void assertAccess(IdentityConflict conflict) {
        if (accessScope.isPlatformWide()) return;
        UUID own = accessScope.requirePrincipal().getInstitutionId();
        boolean visible = own != null && (
                (conflict.getPrimaryStudentId() != null && enrolmentRepository.existsByStudentIdAndInstitutionId(conflict.getPrimaryStudentId(), own))
                || (conflict.getSecondaryStudentId() != null && enrolmentRepository.existsByStudentIdAndInstitutionId(conflict.getSecondaryStudentId(), own)));
        if (!visible) {
            throw new ResourceNotFoundException("IdentityConflict", conflict.getId());
        }
    }

    private IdentityDtos.ConflictDto toDto(IdentityConflict c) {
        String primaryName = c.getPrimaryStudentId() == null ? null
                : studentRepository.findById(c.getPrimaryStudentId()).map(Student::getFullName).orElse(null);
        String secondaryName = c.getSecondaryStudentId() == null ? null
                : studentRepository.findById(c.getSecondaryStudentId()).map(Student::getFullName).orElse(null);
        return new IdentityDtos.ConflictDto(c.getId(), c.getConflictType(), c.getStatus(), c.getSeverity(),
                c.getReasonCode(), c.getDetectionSource(), c.getPrimaryStudentId(), primaryName,
                c.getSecondaryStudentId(), secondaryName, c.getPrimaryEnrolmentId(), c.getSecondaryEnrolmentId(),
                c.getAssignedTo(), c.getDetails(), c.getCreatedAt(), c.getResolvedAt());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
