package com.apaar.platform.identity;

import com.apaar.platform.academic.AcademicYearRepository;
import com.apaar.platform.academic.ProgrammeRepository;
import com.apaar.platform.apaar.ApaarVerificationRecord;
import com.apaar.platform.apaar.ApaarVerificationRecordRepository;
import com.apaar.platform.audit.AuditService;
import com.apaar.platform.cases.CaseDtos;
import com.apaar.platform.cases.InvestigationCaseRepository;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.institution.Institution;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.UserPrincipal;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IdentityMatchingService {

    private static final Logger log = LoggerFactory.getLogger(IdentityMatchingService.class);
    private static final List<String> ACTIVE_CONFLICT_STATUSES = List.of("OPEN", "INVESTIGATING");

    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final InstitutionRepository institutionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ProgrammeRepository programmeRepository;
    private final IdentityConflictRepository conflictRepository;
    private final IdentityMatchingRunRepository runRepository;
    private final IdentityMatchRepository matchRepository;
    private final ApaarVerificationRecordRepository verificationRecordRepository;
    private final InvestigationCaseRepository caseRepository;
    private final AccessScopeService accessScope;
    private final AuditService auditService;
    private final Clock clock;

    public IdentityMatchingService(StudentRepository studentRepository,
                                   EnrolmentRepository enrolmentRepository,
                                   InstitutionRepository institutionRepository,
                                   AcademicYearRepository academicYearRepository,
                                   ProgrammeRepository programmeRepository,
                                   IdentityConflictRepository conflictRepository,
                                   IdentityMatchingRunRepository runRepository,
                                   IdentityMatchRepository matchRepository,
                                   ApaarVerificationRecordRepository verificationRecordRepository,
                                   InvestigationCaseRepository caseRepository,
                                   AccessScopeService accessScope,
                                   AuditService auditService,
                                   Clock clock) {
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.institutionRepository = institutionRepository;
        this.academicYearRepository = academicYearRepository;
        this.programmeRepository = programmeRepository;
        this.conflictRepository = conflictRepository;
        this.runRepository = runRepository;
        this.matchRepository = matchRepository;
        this.verificationRecordRepository = verificationRecordRepository;
        this.caseRepository = caseRepository;
        this.accessScope = accessScope;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public IdentityDtos.MatchingRunDto executeMatchingRun(IdentityDtos.MatchingRunRequest request, UserPrincipal actor) {
        Instant startTime = clock.instant();
        log.info("Starting identity matching run triggered by user {}", actor.getUsername());

        UUID yearFilter = request.academicYearId();
        UUID institutionFilter = request.institutionId();

        List<Enrolment> enrolments = enrolmentRepository.findAll().stream()
                .filter(e -> yearFilter == null || yearFilter.equals(e.getAcademicYearId()))
                .toList();

        // An institution-scoped run must still see the student's enrolments at other
        // institutions, otherwise cross-institution matches can never be detected.
        Set<UUID> scopedStudentIds = null;
        if (institutionFilter != null) {
            scopedStudentIds = enrolments.stream()
                    .filter(e -> institutionFilter.equals(e.getInstitutionId()))
                    .map(Enrolment::getStudentId)
                    .collect(Collectors.toSet());
            Set<UUID> finalStudentScope = scopedStudentIds;
            enrolments = enrolments.stream()
                    .filter(e -> finalStudentScope.contains(e.getStudentId()))
                    .toList();
        }
        List<Enrolment> allEnrolments = enrolments;

        List<Student> allStudents;
        if (yearFilter != null || institutionFilter != null) {
            Set<UUID> finalStudentScope = scopedStudentIds != null
                    ? scopedStudentIds
                    : allEnrolments.stream().map(Enrolment::getStudentId).collect(Collectors.toSet());
            allStudents = studentRepository.findAll().stream()
                    .filter(s -> finalStudentScope.contains(s.getId()))
                    .toList();
        } else {
            allStudents = studentRepository.findAll();
        }

        int recordsScanned = allStudents.size() + allEnrolments.size();
        int matchesFound = 0;
        int conflictsGenerated = 0;
        int conflictsSuppressed = 0;

        List<IdentityMatch> pendingMatches = new ArrayList<>();

        // Group enrolments by student
        Map<UUID, List<Enrolment>> enrolmentsByStudent = allEnrolments.stream()
                .collect(Collectors.groupingBy(Enrolment::getStudentId));

        // Group students by normalized APAAR ID (12 digits)
        Map<String, List<Student>> studentsByApaar = allStudents.stream()
                .filter(s -> s.getApaarId() != null && !s.getApaarId().isBlank())
                .collect(Collectors.groupingBy(s -> s.getApaarId().trim()));

        // =========================================================================
        // 1. Cross-Institution Active Enrolment Matching (Same Student across Institutions)
        // =========================================================================
        for (Map.Entry<UUID, List<Enrolment>> entry : enrolmentsByStudent.entrySet()) {
            UUID studentId = entry.getKey();
            List<Enrolment> studentEnrs = entry.getValue();

            // Compare pairs of enrolments for the same student
            for (int i = 0; i < studentEnrs.size(); i++) {
                for (int j = i + 1; j < studentEnrs.size(); j++) {
                    Enrolment e1 = studentEnrs.get(i);
                    Enrolment e2 = studentEnrs.get(j);

                    if (e1.getInstitutionId().equals(e2.getInstitutionId())) {
                        continue; // same institution duplicate handled separately
                    }

                    boolean sameYear = e1.getAcademicYearId().equals(e2.getAcademicYearId());
                    boolean bothActive = isEnrolmentActive(e1) && isEnrolmentActive(e2);

                    if (sameYear && bothActive) {
                        // SCENARIO A: Simultaneous active enrolment in 2 institutions in same year
                        matchesFound++;
                        String fingerprint = buildFingerprint("CROSS_INSTITUTION", e1.getInstitutionId(), e2.getInstitutionId(), studentId, e1.getAcademicYearId());

                        Optional<IdentityConflict> existingConflict = conflictRepository.findFirstByFingerprintAndStatusIn(
                                fingerprint, ACTIVE_CONFLICT_STATUSES);

                        if (existingConflict.isEmpty()) {
                            // Legacy conflicts (e.g. seeded data) may not have a fingerprint yet.
                            existingConflict = conflictRepository.findOpenByEnrolments(
                                    "DUPLICATE_ACTIVE_ENROLMENT", e1.getId(), e2.getId());
                            existingConflict.ifPresent(existing -> {
                                if (existing.getFingerprint() == null) {
                                    existing.setFingerprint(fingerprint);
                                    conflictRepository.save(existing);
                                }
                            });
                        }

                        UUID conflictId = null;
                        if (existingConflict.isPresent()) {
                            conflictsSuppressed++;
                            conflictId = existingConflict.get().getId();
                        } else {
                            IdentityConflict conflict = new IdentityConflict();
                            conflict.setConflictType("CROSS_INSTITUTION_ACTIVE_ENROLMENT");
                            conflict.setPrimaryStudentId(studentId);
                            conflict.setSecondaryStudentId(studentId);
                            conflict.setPrimaryEnrolmentId(e1.getId());
                            conflict.setSecondaryEnrolmentId(e2.getId());
                            conflict.setDetectionSource("RULE");
                            conflict.setStatus("OPEN");
                            conflict.setSeverity("HIGH");
                            conflict.setReasonCode("DUPLICATE_ACTIVE_ENROLMENT");
                            conflict.setFingerprint(fingerprint);

                            Map<String, Object> details = new LinkedHashMap<>();
                            details.put("academicYearId", e1.getAcademicYearId().toString());
                            details.put("institutions", List.of(e1.getInstitutionId().toString(), e2.getInstitutionId().toString()));
                            details.put("explanation", "Student identity has concurrent active enrolments at multiple institutions during academic year " + e1.getAcademicYearId() + ". Review required.");
                            conflict.setDetails(details);

                            IdentityConflict saved = conflictRepository.save(conflict);
                            conflictId = saved != null ? saved.getId() : null;
                            conflictsGenerated++;
                        }

                        IdentityMatch match = new IdentityMatch();
                        match.setMatchType("CROSS_INSTITUTION_ACTIVE_ENROLMENT");
                        match.setMatchingRule("SAME_STUDENT_CROSS_INSTITUTION_ACTIVE_ENROLMENT");
                        match.setPrimaryStudentId(studentId);
                        match.setSecondaryStudentId(studentId);
                        match.setPrimaryEnrolmentId(e1.getId());
                        match.setSecondaryEnrolmentId(e2.getId());
                        match.setConfidenceScore(new BigDecimal("1.0000"));
                        match.setMatchClassification("POTENTIAL_DUPLICATE_ENROLMENT");
                        match.setReviewRequired(true);
                        match.setMatchingFields(List.of("studentId", "academicYearId"));
                        match.setConflictingFields(List.of("institutionId"));
                        match.setConflictId(conflictId);
                        match.setDetails(Map.of("fingerprint", fingerprint, "bothActive", true));
                        pendingMatches.add(match);

                    } else {
                        // SCENARIO B: Legitimate Historical Enrolment / Transfer across years or inactive
                        matchesFound++;
                        IdentityMatch match = new IdentityMatch();
                        match.setMatchType("HISTORICAL_ENROLMENT");
                        match.setMatchingRule("SAME_STUDENT_HISTORICAL_TRANSITION");
                        match.setPrimaryStudentId(studentId);
                        match.setSecondaryStudentId(studentId);
                        match.setPrimaryEnrolmentId(e1.getId());
                        match.setSecondaryEnrolmentId(e2.getId());
                        match.setConfidenceScore(new BigDecimal("1.0000"));
                        match.setMatchClassification("POSSIBLE_HISTORICAL_ENROLMENT");
                        match.setReviewRequired(false); // Legitimate historical; not an automatic violation
                        match.setMatchingFields(List.of("studentId"));
                        match.setConflictingFields(List.of("academicYearId", "status"));
                        match.setDetails(Map.of(
                                "explanation", "Student has historical or sequential transition between institutions. Historical records retained for academic audit.",
                                "e1Status", e1.getStatus(),
                                "e2Status", e2.getStatus()
                        ));
                        pendingMatches.add(match);
                    }
                }
            }
        }

        // =========================================================================
        // 2. Exact APAAR Match & Attribute Collision Across Different Students
        // =========================================================================
        for (Map.Entry<String, List<Student>> entry : studentsByApaar.entrySet()) {
            String apaarId = entry.getKey();
            List<Student> studentsWithApaar = entry.getValue();

            if (studentsWithApaar.size() > 1) {
                for (int i = 0; i < studentsWithApaar.size(); i++) {
                    for (int j = i + 1; j < studentsWithApaar.size(); j++) {
                        Student s1 = studentsWithApaar.get(i);
                        Student s2 = studentsWithApaar.get(j);

                        matchesFound++;
                        double nameSim = StringSimilarityUtil.nameSimilarity(s1.getFullName(), s2.getFullName());
                        boolean dobMatches = Objects.equals(s1.getDateOfBirth(), s2.getDateOfBirth());

                        List<String> matchingFields = new ArrayList<>(List.of("apaarId"));
                        List<String> conflictingFields = new ArrayList<>();

                        if (nameSim >= 0.85) {
                            matchingFields.add("fullName");
                        } else {
                            conflictingFields.add("fullName");
                        }

                        if (dobMatches) {
                            matchingFields.add("dateOfBirth");
                        } else {
                            conflictingFields.add("dateOfBirth");
                        }

                        String matchRule;
                        String classification;
                        String severity = "HIGH";

                        if (!conflictingFields.isEmpty()) {
                            // SCENARIO D: Conflicting Identity Attributes
                            matchRule = "APAAR_ATTRIBUTE_COLLISION";
                            classification = "IDENTITY_ATTRIBUTE_CONFLICT";
                        } else {
                            matchRule = "EXACT_APAAR_DUPLICATE_STUDENT";
                            classification = "POTENTIAL_DUPLICATE_ENROLMENT";
                        }

                        String fingerprint = buildFingerprint("APAAR_MATCH", s1.getId(), s2.getId(), apaarId);
                        Optional<IdentityConflict> existingConflict = conflictRepository.findFirstByFingerprintAndStatusIn(
                                fingerprint, ACTIVE_CONFLICT_STATUSES);

                        if (existingConflict.isEmpty()) {
                            String reasonCode = conflictingFields.isEmpty()
                                    ? "APAAR_ALREADY_ASSIGNED" : "IDENTITY_ATTRIBUTE_MISMATCH";
                            existingConflict = conflictRepository.findOpenByStudents(
                                    reasonCode, s1.getId(), s2.getId());
                            existingConflict.ifPresent(existing -> {
                                if (existing.getFingerprint() == null) {
                                    existing.setFingerprint(fingerprint);
                                    conflictRepository.save(existing);
                                }
                            });
                        }

                        UUID conflictId = null;
                        if (existingConflict.isPresent()) {
                            conflictsSuppressed++;
                            conflictId = existingConflict.get().getId();
                        } else {
                            IdentityConflict conflict = new IdentityConflict();
                            conflict.setConflictType("APAAR_IDENTITY_CONFLICT");
                            conflict.setPrimaryStudentId(s1.getId());
                            conflict.setSecondaryStudentId(s2.getId());
                            conflict.setDetectionSource("RULE");
                            conflict.setStatus("OPEN");
                            conflict.setSeverity(severity);
                            conflict.setReasonCode(conflictingFields.isEmpty() ? "APAAR_ALREADY_ASSIGNED" : "IDENTITY_ATTRIBUTE_MISMATCH");
                            conflict.setFingerprint(fingerprint);

                            Map<String, Object> details = new LinkedHashMap<>();
                            details.put("apaarId", apaarId);
                            details.put("nameSimilarity", BigDecimal.valueOf(nameSim).setScale(4, RoundingMode.HALF_UP).doubleValue());
                            details.put("conflictingFields", conflictingFields);
                            details.put("s1Name", s1.getFullName());
                            details.put("s2Name", s2.getFullName());
                            details.put("s1Dob", s1.getDateOfBirth() != null ? s1.getDateOfBirth().toString() : null);
                            details.put("s2Dob", s2.getDateOfBirth() != null ? s2.getDateOfBirth().toString() : null);
                            details.put("explanation", "The same 12-digit APAAR identifier is claimed by two distinct student records. Attribute alignment: name similarity " + String.format("%.2f", nameSim));
                            conflict.setDetails(details);

                            IdentityConflict saved = conflictRepository.save(conflict);
                            conflictId = saved != null ? saved.getId() : null;
                            conflictsGenerated++;
                        }

                        IdentityMatch match = new IdentityMatch();
                        match.setMatchType("APAAR_IDENTITY_CONFLICT");
                        match.setMatchingRule(matchRule);
                        match.setPrimaryStudentId(s1.getId());
                        match.setSecondaryStudentId(s2.getId());
                        match.setConfidenceScore(BigDecimal.valueOf(nameSim).setScale(4, RoundingMode.HALF_UP));
                        match.setMatchClassification(classification);
                        match.setReviewRequired(true);
                        match.setMatchingFields(matchingFields);
                        match.setConflictingFields(conflictingFields);
                        match.setConflictId(conflictId);
                        match.setDetails(Map.of("apaarId", apaarId, "nameSimilarity", nameSim, "fingerprint", fingerprint));
                        pendingMatches.add(match);
                    }
                }
            }
        }

        // =========================================================================
        // 3. Save Matching Run and Match Records
        // =========================================================================
        Instant endTime = clock.instant();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("recordsScanned", recordsScanned);
        summary.put("matchesFound", matchesFound);
        summary.put("conflictsGenerated", conflictsGenerated);
        summary.put("conflictsSuppressed", conflictsSuppressed);
        summary.put("totalStudents", allStudents.size());
        summary.put("totalEnrolments", allEnrolments.size());

        IdentityMatchingRun run = new IdentityMatchingRun(
                request.runType() != null ? request.runType() : "EXACT_MATCH",
                recordsScanned,
                matchesFound,
                conflictsGenerated,
                conflictsSuppressed,
                actor.getUserId(),
                startTime,
                endTime,
                summary
        );
        IdentityMatchingRun savedRun = runRepository.save(run);

        // Assign run ID and persist all match records
        for (IdentityMatch m : pendingMatches) {
            m.setRunId(savedRun.getId());
        }
        matchRepository.saveAll(pendingMatches);

        auditService.record("IDENTITY_MATCHING_RUN_COMPLETED", "IDENTITY_MATCHING_RUN", savedRun.getId().toString(),
                "EXECUTE", "SUCCESS", null, summary);

        return toRunDto(savedRun);
    }

    @Transactional(readOnly = true)
    public PageResponse<IdentityDtos.MatchingRunDto> listMatchingRuns(Pageable pageable) {
        Page<IdentityMatchingRun> runs = runRepository.findAllByOrderByStartedAtDesc(pageable);
        return PageResponse.from(runs, this::toRunDto);
    }

    @Transactional(readOnly = true)
    public IdentityDtos.MatchingRunDto getMatchingRun(UUID id) {
        IdentityMatchingRun run = runRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MATCHING_RUN_NOT_FOUND", "Matching run not found: " + id));
        return toRunDto(run);
    }

    @Transactional(readOnly = true)
    public PageResponse<IdentityDtos.IdentityMatchDto> listMatches(String classification, Pageable pageable) {
        String cleanClass = (classification != null && !classification.isBlank() && !"ALL".equalsIgnoreCase(classification))
                ? classification.trim().toUpperCase() : null;
        Page<IdentityMatch> matches = matchRepository.search(cleanClass,
                accessScope.resolveInstitutionFilter(null), pageable);
        return PageResponse.from(matches, this::toMatchDto);
    }

    @Transactional(readOnly = true)
    public IdentityDtos.IdentityMatchDto getMatch(UUID id) {
        IdentityMatch m = matchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MATCH_NOT_FOUND", "Identity match not found: " + id));
        assertMatchAccess(m);
        return toMatchDto(m);
    }

    @Transactional(readOnly = true)
    public List<IdentityDtos.IdentityMatchDto> getMatchesForStudent(UUID studentId) {
        assertStudentAccess(studentId);
        return matchRepository.findByStudentId(studentId).stream()
                .map(this::toMatchDto)
                .toList();
    }

    private void assertMatchAccess(IdentityMatch match) {
        if (accessScope.isPlatformWide()) {
            return;
        }
        UUID own = accessScope.requirePrincipal().getInstitutionId();
        boolean visible = own != null && (
                (match.getPrimaryStudentId() != null
                        && enrolmentRepository.existsByStudentIdAndInstitutionId(match.getPrimaryStudentId(), own))
                        || (match.getSecondaryStudentId() != null
                        && enrolmentRepository.existsByStudentIdAndInstitutionId(match.getSecondaryStudentId(), own)));
        if (!visible) {
            throw new ResourceNotFoundException("MATCH_NOT_FOUND", "Identity match not found: " + match.getId());
        }
    }

    private void assertStudentAccess(UUID studentId) {
        if (accessScope.isPlatformWide()) {
            return;
        }
        UUID own = accessScope.requirePrincipal().getInstitutionId();
        if (own == null || !enrolmentRepository.existsByStudentIdAndInstitutionId(studentId, own)) {
            throw new ResourceNotFoundException("STUDENT_NOT_FOUND", "Student not found: " + studentId);
        }
    }

    @Transactional(readOnly = true)
    public IdentityDtos.ConflictEvidenceDto getConflictEvidence(UUID conflictId) {
        IdentityConflict conflict = conflictRepository.findById(conflictId)
                .orElseThrow(() -> new ResourceNotFoundException("CONFLICT_NOT_FOUND", "Identity conflict not found: " + conflictId));

        Student primaryStudent = conflict.getPrimaryStudentId() != null
                ? studentRepository.findById(conflict.getPrimaryStudentId()).orElse(null) : null;
        Student secondaryStudent = conflict.getSecondaryStudentId() != null
                ? studentRepository.findById(conflict.getSecondaryStudentId()).orElse(null) : null;

        Enrolment primaryEnr = conflict.getPrimaryEnrolmentId() != null
                ? enrolmentRepository.findById(conflict.getPrimaryEnrolmentId()).orElse(null) : null;
        Enrolment secondaryEnr = conflict.getSecondaryEnrolmentId() != null
                ? enrolmentRepository.findById(conflict.getSecondaryEnrolmentId()).orElse(null) : null;

        Map<String, Object> primaryRecord = buildRecordSnapshot(primaryStudent, primaryEnr);
        Map<String, Object> secondaryRecord = buildRecordSnapshot(secondaryStudent, secondaryEnr);

        List<String> matchingFields = new ArrayList<>();
        List<String> conflictingFields = new ArrayList<>();

        if (primaryStudent != null && secondaryStudent != null) {
            if (Objects.equals(primaryStudent.getApaarId(), secondaryStudent.getApaarId())) {
                matchingFields.add("apaarId");
            }
            if (Objects.equals(primaryStudent.getFullName(), secondaryStudent.getFullName())) {
                matchingFields.add("fullName");
            } else {
                conflictingFields.add("fullName");
            }
            if (Objects.equals(primaryStudent.getDateOfBirth(), secondaryStudent.getDateOfBirth())) {
                matchingFields.add("dateOfBirth");
            } else {
                conflictingFields.add("dateOfBirth");
            }
        }

        if (primaryEnr != null && secondaryEnr != null) {
            if (primaryEnr.getAcademicYearId().equals(secondaryEnr.getAcademicYearId())) {
                matchingFields.add("academicYearId");
            } else {
                conflictingFields.add("academicYearId");
            }
            if (!primaryEnr.getInstitutionId().equals(secondaryEnr.getInstitutionId())) {
                conflictingFields.add("institutionId");
            }
        }

        String explanation = (String) conflict.getDetails().getOrDefault("explanation",
                "Conflict flagged based on deterministic identifier match and enrolment status comparison.");

        List<IdentityDtos.VerificationRecordDto> verificationHistory = new ArrayList<>();
        if (primaryStudent != null) {
            verificationRecordRepository.findByStudentIdOrderByRequestedAtDesc(primaryStudent.getId()).forEach(vr ->
                    verificationHistory.add(new IdentityDtos.VerificationRecordDto(
                            vr.getId(),
                            primaryStudent.getId(),
                            primaryStudent.getFullName(),
                            vr.getApaarId(),
                            vr.getStatus(),
                            vr.getVerificationMode(),
                            vr.getAdapterName(),
                            vr.getProviderReference(),
                            vr.getReasonCode(),
                            vr.isSimulated(),
                            !vr.isSimulated(),
                            "Verification record for primary student",
                            vr.getVerifiedAt(),
                            vr.getRequestedAt()
                    ))
            );
        }
        if (secondaryStudent != null && (primaryStudent == null || !secondaryStudent.getId().equals(primaryStudent.getId()))) {
            verificationRecordRepository.findByStudentIdOrderByRequestedAtDesc(secondaryStudent.getId()).forEach(vr ->
                    verificationHistory.add(new IdentityDtos.VerificationRecordDto(
                            vr.getId(),
                            secondaryStudent.getId(),
                            secondaryStudent.getFullName(),
                            vr.getApaarId(),
                            vr.getStatus(),
                            vr.getVerificationMode(),
                            vr.getAdapterName(),
                            vr.getProviderReference(),
                            vr.getReasonCode(),
                            vr.isSimulated(),
                            !vr.isSimulated(),
                            "Verification record for secondary student",
                            vr.getVerifiedAt(),
                            vr.getRequestedAt()
                    ))
            );
        }

        return new IdentityDtos.ConflictEvidenceDto(
                conflict.getId(),
                conflict.getConflictType(),
                conflict.getReasonCode(),
                new BigDecimal("1.0000"),
                primaryRecord,
                secondaryRecord,
                matchingFields,
                conflictingFields,
                verificationHistory,
                explanation
        );
    }

    private Map<String, Object> buildRecordSnapshot(Student student, Enrolment enrolment) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (student != null) {
            map.put("studentId", student.getId());
            map.put("fullName", student.getFullName());
            map.put("apaarId", student.getApaarId());
            map.put("apaarFormatValid", student.isApaarFormatValid());
            map.put("dateOfBirth", student.getDateOfBirth());
            map.put("gender", student.getGender());
            map.put("category", student.getCategory());
        }
        if (enrolment != null) {
            map.put("enrolmentId", enrolment.getId());
            map.put("enrolmentNumber", enrolment.getEnrolmentNumber());
            map.put("institutionId", enrolment.getInstitutionId());
            map.put("institutionName", institutionRepository.findById(enrolment.getInstitutionId()).map(Institution::getName).orElse(null));
            map.put("academicYearId", enrolment.getAcademicYearId());
            map.put("programmeId", enrolment.getProgrammeId());
            map.put("status", enrolment.getStatus());
            map.put("admissionDate", enrolment.getAdmissionDate());
        }
        return map;
    }

    @Transactional(readOnly = true)
    public IdentityDtos.IdentityDashboardDto getDashboardMetrics() {
        UUID institutionId = accessScope.isPlatformWide()
                ? null
                : accessScope.requirePrincipal().getInstitutionId();

        long totalStudents = institutionId == null
                ? studentRepository.count()
                : studentRepository.countDistinctByInstitutionId(institutionId);
        long formatValid = studentRepository.countApaarFormatValid(institutionId);
        long missingOrMalformed = totalStudents - formatValid;

        long openConflicts = conflictRepository.countByStatusInAndInstitutionId(ACTIVE_CONFLICT_STATUSES, institutionId);

        long authVerified = verificationRecordRepository.countByStatusAndVerificationModeAndInstitutionId(
                "VERIFIED", "AUTHORIZED", institutionId);
        long simVerified = verificationRecordRepository.countByStatusAndVerificationModeAndInstitutionId(
                "VERIFIED", "SIMULATED", institutionId);
        long totalVerified = verificationRecordRepository.countByStatusAndInstitutionId("VERIFIED", institutionId);
        long verificationPending = Math.max(0, formatValid - totalVerified);

        long potentialDuplicates = matchRepository.countDistinctConflictsByMatchClassification(
                "POTENTIAL_DUPLICATE_ENROLMENT", institutionId);
        long casesAwaiting = caseRepository.countByStatusInAndInstitutionId(
                List.of("OPEN", "ASSIGNED", "IN_REVIEW", "PENDING_INFO"), institutionId);
        long resolvedCases = caseRepository.countByStatusInAndInstitutionId(
                List.of("RESOLVED", "CLOSED"), institutionId);

        return new IdentityDtos.IdentityDashboardDto(
                totalStudents,
                formatValid,
                missingOrMalformed,
                verificationPending,
                authVerified,
                simVerified,
                potentialDuplicates,
                openConflicts,
                casesAwaiting,
                resolvedCases
        );
    }

    private boolean isEnrolmentActive(Enrolment e) {
        return e.getStatus() != null && ("ACTIVE".equalsIgnoreCase(e.getStatus()) || "PROVISIONAL".equalsIgnoreCase(e.getStatus()));
    }

    private String buildFingerprint(String prefix, Object... parts) {
        List<String> strParts = Arrays.stream(parts)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .sorted()
                .toList();
        String raw = String.join(":", strParts);
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return prefix + ":" + hex.substring(0, 32);
        } catch (Exception e) {
            return prefix + ":" + Integer.toHexString(raw.hashCode());
        }
    }

    private IdentityDtos.MatchingRunDto toRunDto(IdentityMatchingRun r) {
        return new IdentityDtos.MatchingRunDto(
                r.getId(),
                r.getRunType(),
                r.getStatus(),
                r.getRecordsScanned(),
                r.getMatchesFound(),
                r.getConflictsGenerated(),
                r.getConflictsSuppressed(),
                r.getRunBy(),
                r.getStartedAt(),
                r.getCompletedAt(),
                r.getSummary()
        );
    }

    private IdentityDtos.IdentityMatchDto toMatchDto(IdentityMatch m) {
        String pStudentName = m.getPrimaryStudentId() != null
                ? studentRepository.findById(m.getPrimaryStudentId()).map(Student::getFullName).orElse(null) : null;
        String sStudentName = m.getSecondaryStudentId() != null
                ? studentRepository.findById(m.getSecondaryStudentId()).map(Student::getFullName).orElse(null) : null;

        String pInstName = null;
        if (m.getPrimaryEnrolmentId() != null) {
            pInstName = enrolmentRepository.findById(m.getPrimaryEnrolmentId())
                    .flatMap(e -> institutionRepository.findById(e.getInstitutionId()))
                    .map(Institution::getName).orElse(null);
        }

        String sInstName = null;
        if (m.getSecondaryEnrolmentId() != null) {
            sInstName = enrolmentRepository.findById(m.getSecondaryEnrolmentId())
                    .flatMap(e -> institutionRepository.findById(e.getInstitutionId()))
                    .map(Institution::getName).orElse(null);
        }

        return new IdentityDtos.IdentityMatchDto(
                m.getId(),
                m.getRunId(),
                m.getMatchType(),
                m.getMatchingRule(),
                m.getPrimaryStudentId(),
                pStudentName,
                m.getPrimaryEnrolmentId(),
                pInstName,
                m.getSecondaryStudentId(),
                sStudentName,
                m.getSecondaryEnrolmentId(),
                sInstName,
                m.getConfidenceScore(),
                m.getMatchClassification(),
                m.isReviewRequired(),
                m.getMatchingFields(),
                m.getConflictingFields(),
                m.getDetails(),
                m.getConflictId(),
                m.getCreatedAt()
        );
    }
}
