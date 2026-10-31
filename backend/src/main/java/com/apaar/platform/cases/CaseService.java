package com.apaar.platform.cases;

import com.apaar.platform.audit.AuditService;
import com.apaar.platform.cases.CaseDtos.*;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.common.exception.UnprocessableEntityException;
import com.apaar.platform.identity.IdentityConflict;
import com.apaar.platform.identity.IdentityConflictRepository;
import com.apaar.platform.institution.Institution;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.User;
import com.apaar.platform.security.UserPrincipal;
import com.apaar.platform.security.UserRepository;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.StudentRepository;
import com.apaar.platform.student.StudentRecordVersion;
import com.apaar.platform.student.StudentRecordVersionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CaseService {

    private static final List<String> VALID_CASE_STATUSES =
            List.of("OPEN", "ASSIGNED", "IN_REVIEW", "PENDING_INFO", "RESOLVED", "CLOSED");

    private final InvestigationCaseRepository caseRepository;
    private final CaseCommentRepository commentRepository;
    private final CaseEvidenceRepository evidenceRepository;
    private final CaseDecisionRepository decisionRepository;
    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final AccessScopeService accessScope;
    private final AuditService auditService;
    private final IdentityConflictRepository conflictRepository;
    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final StudentRecordVersionRepository studentRecordVersionRepository;

    public CaseService(InvestigationCaseRepository caseRepository,
                       CaseCommentRepository commentRepository,
                       CaseEvidenceRepository evidenceRepository,
                       CaseDecisionRepository decisionRepository,
                       InstitutionRepository institutionRepository,
                       UserRepository userRepository,
                       AccessScopeService accessScope,
                       AuditService auditService,
                       IdentityConflictRepository conflictRepository,
                       StudentRepository studentRepository,
                       EnrolmentRepository enrolmentRepository,
                       StudentRecordVersionRepository studentRecordVersionRepository) {
        this.caseRepository = caseRepository;
        this.commentRepository = commentRepository;
        this.evidenceRepository = evidenceRepository;
        this.decisionRepository = decisionRepository;
        this.institutionRepository = institutionRepository;
        this.userRepository = userRepository;
        this.accessScope = accessScope;
        this.auditService = auditService;
        this.conflictRepository = conflictRepository;
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.studentRecordVersionRepository = studentRecordVersionRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseSummaryDto> listCases(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size);
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        UUID institutionFilter = null;
        if (!accessScope.isPlatformWide()) {
            institutionFilter = accessScope.requirePrincipal().getInstitutionId();
        }

        Page<InvestigationCase> casePage = caseRepository.search(institutionFilter, cleanStatus, pageable);
        return PageResponse.from(casePage, this::toSummaryDto);
    }

    @Transactional(readOnly = true)
    public CaseDetailDto getCase(UUID id) {
        InvestigationCase c = caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "Investigation case not found: " + id));

        assertAccess(c);

        String instName = c.getInstitutionId() != null
                ? institutionRepository.findById(c.getInstitutionId()).map(Institution::getName).orElse(null)
                : null;

        String assigneeName = c.getAssignedTo() != null
                ? userRepository.findById(c.getAssignedTo()).map(User::getFullName).orElse(null)
                : null;

        List<CaseEvidenceDto> evidenceDtos = evidenceRepository.findByCaseIdOrderByCollectedAtDesc(c.getId()).stream()
                .map(e -> new CaseEvidenceDto(e.getId(), e.getEvidenceType(), e.getDescription(), e.getStorageRef(), e.getCreatedAt()))
                .toList();

        List<CaseCommentDto> commentDtos = commentRepository.findByCaseIdOrderByCreatedAtAsc(c.getId()).stream()
                .map(cm -> {
                    String authorName = userRepository.findById(cm.getAuthorId()).map(User::getFullName).orElse("System User");
                    String authorEmail = userRepository.findById(cm.getAuthorId()).map(User::getEmail).orElse(null);
                    return new CaseCommentDto(cm.getId(), authorName, authorEmail, cm.getBody(), cm.getCreatedAt());
                })
                .toList();

        List<CaseDecisionDto> decisionDtos = decisionRepository.findByCaseIdOrderByDecidedAtDesc(c.getId()).stream()
                .map(d -> {
                    String decidedByName = userRepository.findById(d.getDecidedBy()).map(User::getFullName).orElse("Officer");
                    return new CaseDecisionDto(d.getId(), d.getDecisionCode(), d.getRationale(), decidedByName, d.getCreatedAt());
                })
                .toList();

        return new CaseDetailDto(
                c.getId(),
                c.getCaseNumber(),
                c.getTitle(),
                c.getCaseType(),
                c.getStatus(),
                c.getPriority(),
                instName,
                assigneeName,
                c.getOpenedAt(),
                c.getCreatedAt(),
                c.getDescription(),
                evidenceDtos,
                commentDtos,
                decisionDtos
        );
    }

    @Transactional
    public CaseCommentDto addComment(UUID caseId, AddCommentRequest request, UserPrincipal actor) {
        InvestigationCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "Investigation case not found: " + caseId));

        assertAccess(c);

        CaseComment comment = new CaseComment();
        comment.setCaseId(c.getId());
        comment.setAuthorId(actor.getUserId());
        comment.setBody(request.body().trim());
        comment.setInternal(true);
        comment = commentRepository.save(comment);

        return new CaseCommentDto(
                comment.getId(),
                actor.getFullName(),
                actor.getEmail(),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }

    @Transactional
    public CaseDetailDto createCaseFromConflict(UUID conflictId, UserPrincipal actor) {
        IdentityConflict conflict = conflictRepository.findById(conflictId)
                .orElseThrow(() -> new ResourceNotFoundException("CONFLICT_NOT_FOUND", "Identity conflict not found: " + conflictId));
        assertConflictAccess(conflict);

        String caseNumber = "CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        InvestigationCase c = new InvestigationCase();
        c.setCaseNumber(caseNumber);
        c.setCaseType("IDENTITY_CONFLICT");
        c.setTitle("Identity Conflict Review: " + conflict.getConflictType() + " [" + conflict.getReasonCode() + "]");
        String desc = "Formal investigation opened for conflict " + conflictId + ". " 
                + (conflict.getDetails() != null ? conflict.getDetails().getOrDefault("explanation", "") : "");
        c.setDescription(desc);
        c.setPriority(conflict.getSeverity() != null ? conflict.getSeverity() : "HIGH");
        c.setStatus("OPEN");
        c.setOpenedBy(actor.getUserId());
        c.setOpenedAt(Instant.now());

        if (conflict.getPrimaryEnrolmentId() != null) {
            var enr = enrolmentRepository.findById(conflict.getPrimaryEnrolmentId()).orElse(null);
            if (enr != null) {
                c.setInstitutionId(enr.getInstitutionId());
            }
        }

        c = caseRepository.save(c);

        CaseEvidence evidence = new CaseEvidence();
        evidence.setCaseId(c.getId());
        evidence.setEvidenceType("IDENTITY_CONFLICT_DOSSIER");
        evidence.setDescription("System-linked conflict dossier for " + conflict.getConflictType() + " (" + conflict.getReasonCode() + ")");
        evidence.setStorageRef("conflict:" + conflict.getId());
        evidence.setCollectedBy(actor.getUserId());
        evidence.setCollectedAt(Instant.now());
        evidenceRepository.save(evidence);

        conflict.setStatus("INVESTIGATING");
        conflict.setAssignedTo(actor.getUserId());
        conflictRepository.save(conflict);

        auditService.record("INVESTIGATION_CASE_CREATED", "CASE", c.getId().toString(),
                "CREATE", "SUCCESS", c.getInstitutionId(), Map.of("conflictId", conflictId.toString(), "caseNumber", caseNumber));

        return getCase(c.getId());
    }

    @Transactional
    public CaseDetailDto recordDecision(UUID caseId, RecordDecisionRequest request, UserPrincipal actor) {
        InvestigationCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "Investigation case not found: " + caseId));

        assertAccess(c);

        CaseDecision decision = new CaseDecision();
        decision.setCaseId(c.getId());
        decision.setDecisionCode(request.decisionCode());
        decision.setRationale(request.rationale());
        decision.setEffectiveAction(request.effectiveAction());
        decision.setDecidedBy(actor.getUserId());
        decision.setDecidedAt(Instant.now());
        decisionRepository.save(decision);

        c.setStatus("RESOLVED");
        c.setClosedAt(Instant.now());
        c.setResolutionSummary(request.rationale());
        caseRepository.save(c);

        // Resolve linked conflicts if any evidence references a conflict
        List<CaseEvidence> evidenceList = evidenceRepository.findByCaseIdOrderByCollectedAtDesc(c.getId());
        for (CaseEvidence ev : evidenceList) {
            if (ev.getStorageRef() != null && ev.getStorageRef().startsWith("conflict:")) {
                String confIdStr = ev.getStorageRef().substring("conflict:".length()).trim();
                try {
                    UUID confId = UUID.fromString(confIdStr);
                    conflictRepository.findById(confId).ifPresent(conflict -> {
                        conflict.setStatus("RESOLVED");
                        conflict.setResolvedAt(Instant.now());
                        conflict.setResolvedBy(actor.getUserId());
                        conflict.setResolutionNotes("Resolved via Case " + c.getCaseNumber() + ": [" + request.decisionCode() + "] " + request.rationale());
                        conflictRepository.save(conflict);
                    });
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Apply student correction if specified
        if (request.studentIdToCorrect() != null) {
            studentRepository.findById(request.studentIdToCorrect()).ifPresent(st -> {
                int nextVer = studentRecordVersionRepository.findLatestVersionNo(st.getId()) + 1;
                Map<String, Object> prevSnapshot = new LinkedHashMap<>();
                prevSnapshot.put("fullName", st.getFullName());
                prevSnapshot.put("apaarId", st.getApaarId());
                prevSnapshot.put("dateOfBirth", st.getDateOfBirth());
                prevSnapshot.put("gender", st.getGender());
                prevSnapshot.put("category", st.getCategory());

                StudentRecordVersion ver = new StudentRecordVersion(
                        st.getId(),
                        nextVer,
                        prevSnapshot,
                        "Record correction applied via case decision " + request.decisionCode() + ": " + request.rationale(),
                        actor.getUserId()
                );
                studentRecordVersionRepository.save(ver);

                if (request.correctedFullName() != null && !request.correctedFullName().isBlank()) {
                    st.setFullName(request.correctedFullName().trim());
                }
                if (request.correctedDateOfBirth() != null && !request.correctedDateOfBirth().isBlank()) {
                    st.setDateOfBirth(java.time.LocalDate.parse(request.correctedDateOfBirth().trim()));
                }
                if (request.correctedGender() != null && !request.correctedGender().isBlank()) {
                    st.setGender(request.correctedGender().trim().toUpperCase());
                }
                studentRepository.save(st);
            });
        }

        auditService.record("CASE_DECIDED", "CASE", c.getId().toString(),
                "DECIDE", "SUCCESS", c.getInstitutionId(), Map.of("decisionCode", request.decisionCode(), "caseNumber", c.getCaseNumber()));

        return getCase(c.getId());
    }

    @Transactional
    public CaseDetailDto updateStatus(UUID caseId, UpdateCaseStatusRequest request, UserPrincipal actor) {
        InvestigationCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "Investigation case not found: " + caseId));
        assertAccess(c);

        String newStatus = request.status().trim().toUpperCase();
        if ("UNDER_INVESTIGATION".equals(newStatus)) {
            newStatus = "IN_REVIEW";
        }
        if (!VALID_CASE_STATUSES.contains(newStatus)) {
            throw new UnprocessableEntityException("INVALID_CASE_STATUS",
                    "Unsupported case status: " + request.status());
        }
        c.setStatus(newStatus);
        if ("RESOLVED".equals(newStatus) || "CLOSED".equals(newStatus)) {
            if (c.getClosedAt() == null) c.setClosedAt(Instant.now());
            if (request.notes() != null && !request.notes().isBlank()) {
                c.setResolutionSummary(request.notes().trim());
            }
        }
        caseRepository.save(c);

        auditService.record("CASE_STATUS_UPDATED", "CASE", c.getId().toString(),
                "UPDATE", "SUCCESS", c.getInstitutionId(), Map.of("status", newStatus));

        return getCase(c.getId());
    }

    @Transactional
    public CaseDetailDto assignCase(UUID caseId, AssignCaseRequest request, UserPrincipal actor) {
        InvestigationCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "Investigation case not found: " + caseId));
        assertAccess(c);

        c.setAssignedTo(request.assignedTo());
        caseRepository.save(c);

        auditService.record("CASE_ASSIGNED", "CASE", c.getId().toString(),
                "UPDATE", "SUCCESS", c.getInstitutionId(), Map.of("assignedTo", String.valueOf(request.assignedTo())));

        return getCase(c.getId());
    }

    @Transactional
    public CaseEvidenceDto addEvidence(UUID caseId, AddEvidenceRequest request, UserPrincipal actor) {
        InvestigationCase c = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("CASE_NOT_FOUND", "Investigation case not found: " + caseId));
        assertAccess(c);

        CaseEvidence ev = new CaseEvidence();
        ev.setCaseId(c.getId());
        ev.setEvidenceType(request.type().trim());
        ev.setDescription(request.description().trim());
        ev.setStorageRef(request.storageRef());
        ev.setCollectedBy(actor.getUserId());
        ev.setCollectedAt(Instant.now());
        ev = evidenceRepository.save(ev);

        auditService.record("CASE_EVIDENCE_ADDED", "CASE", c.getId().toString(),
                "CREATE", "SUCCESS", c.getInstitutionId(), Map.of("evidenceType", ev.getEvidenceType()));

        return new CaseEvidenceDto(ev.getId(), ev.getEvidenceType(), ev.getDescription(), ev.getStorageRef(), ev.getCreatedAt());
    }

    private CaseSummaryDto toSummaryDto(InvestigationCase c) {
        String instName = c.getInstitutionId() != null
                ? institutionRepository.findById(c.getInstitutionId()).map(Institution::getName).orElse(null)
                : null;

        String assigneeName = c.getAssignedTo() != null
                ? userRepository.findById(c.getAssignedTo()).map(User::getFullName).orElse(null)
                : null;

        return new CaseSummaryDto(
                c.getId(),
                c.getCaseNumber(),
                c.getTitle(),
                c.getCaseType(),
                c.getStatus(),
                c.getPriority(),
                instName,
                assigneeName,
                c.getOpenedAt(),
                c.getCreatedAt()
        );
    }

    private void assertAccess(InvestigationCase c) {
        if (accessScope.isPlatformWide()) return;
        UUID own = accessScope.requirePrincipal().getInstitutionId();
        if (c.getInstitutionId() != null && !c.getInstitutionId().equals(own)) {
            throw new ResourceNotFoundException("CASE_NOT_FOUND", "Investigation case not found: " + c.getId());
        }
    }

    private void assertConflictAccess(IdentityConflict conflict) {
        if (accessScope.isPlatformWide()) return;
        UUID own = accessScope.requirePrincipal().getInstitutionId();
        boolean visible = own != null && (
                (conflict.getPrimaryStudentId() != null
                        && enrolmentRepository.existsByStudentIdAndInstitutionId(conflict.getPrimaryStudentId(), own))
                        || (conflict.getSecondaryStudentId() != null
                        && enrolmentRepository.existsByStudentIdAndInstitutionId(conflict.getSecondaryStudentId(), own)));
        if (!visible) {
            throw new ResourceNotFoundException("CONFLICT_NOT_FOUND", "Identity conflict not found: " + conflict.getId());
        }
    }
}
