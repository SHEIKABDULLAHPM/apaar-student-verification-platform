package com.apaar.platform.ingestion.service;

import com.apaar.platform.ingestion.DataImportJob;
import com.apaar.platform.ingestion.DataImportRow;
import com.apaar.platform.ingestion.DataImportRowRepository;
import com.apaar.platform.ingestion.dto.ImportDtos.ReconciliationItemDto;
import com.apaar.platform.ingestion.dto.ImportDtos.ReconciliationSummaryDto;
import com.apaar.platform.student.Enrolment;
import com.apaar.platform.student.EnrolmentRepository;
import com.apaar.platform.student.Student;
import com.apaar.platform.student.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ReconciliationService {

    private final EnrolmentRepository enrolmentRepository;
    private final StudentRepository studentRepository;
    private final DataImportRowRepository rowRepository;

    public ReconciliationService(EnrolmentRepository enrolmentRepository,
                                 StudentRepository studentRepository,
                                 DataImportRowRepository rowRepository) {
        this.enrolmentRepository = enrolmentRepository;
        this.studentRepository = studentRepository;
        this.rowRepository = rowRepository;
    }

    @Transactional(readOnly = true)
    public ReconciliationSummaryDto computeReconciliation(DataImportJob job) {
        UUID institutionId = job.getInstitutionId();
        UUID academicYearId = job.getAcademicYearId();
        UUID programmeId = job.getProgrammeId();

        // 1. Fetch all enrolments for institution and academic year (optionally filtered by programme).
        //    Commit matches rows against every status, so reconciliation must use the same lookup scope.
        List<Enrolment> existingEnrolments;
        if (programmeId != null) {
            existingEnrolments = enrolmentRepository.findByInstitutionIdAndAcademicYearId(institutionId, academicYearId).stream()
                    .filter(e -> programmeId.equals(e.getProgrammeId()))
                    .toList();
        } else {
            existingEnrolments = enrolmentRepository.findByInstitutionIdAndAcademicYearId(institutionId, academicYearId);
        }

        Map<String, Enrolment> prevEnrolmentMap = new HashMap<>();
        Map<String, Enrolment> activeSnapshotMap = new HashMap<>();
        Map<UUID, Student> prevStudentMap = new HashMap<>();
        for (Enrolment e : existingEnrolments) {
            String key = e.getEnrolmentNumber().trim().toUpperCase();
            prevEnrolmentMap.putIfAbsent(key, e);
            if (List.of("ACTIVE", "PROVISIONAL").contains(e.getStatus())) {
                activeSnapshotMap.putIfAbsent(key, e);
            }
            studentRepository.findById(e.getStudentId()).ifPresent(s -> prevStudentMap.put(e.getStudentId(), s));
        }

        // 2. Fetch rows of this import job
        List<DataImportRow> rows = rowRepository.findByImportJobIdOrderByRowNumberAsc(job.getId());

        int newCount = 0;
        int unchangedCount = 0;
        int updatedCount = 0;
        List<ReconciliationItemDto> items = new ArrayList<>();
        Set<String> processedIdentifiers = new HashSet<>();

        for (DataImportRow row : rows) {
            if ("INVALID".equals(row.getRowStatus()) || "DUPLICATE".equals(row.getRowStatus())) {
                continue;
            }
            Map<String, Object> payload = row.getNormalizedPayload();
            if (payload == null) continue;

            String identifier = (String) payload.get("studentIdentifier");
            if (identifier == null) continue;
            String key = identifier.trim().toUpperCase();
            processedIdentifiers.add(key);

            String apaarId = (String) payload.get("apaarId");
            String fullName = (String) payload.get("fullName");

            Enrolment prevEnrolment = prevEnrolmentMap.get(key);
            if (prevEnrolment == null) {
                newCount++;
                items.add(new ReconciliationItemDto(
                        identifier,
                        apaarId,
                        fullName,
                        "NEW_ADMISSION",
                        Collections.emptyMap()
                ));
            } else {
                Student prevStudent = prevStudentMap.get(prevEnrolment.getStudentId());
                Map<String, Object> diffs = new HashMap<>();
                if (prevStudent != null) {
                    putDiff(diffs, "fullName", prevStudent.getFullName(), fullName);
                    putDiff(diffs, "apaarId", prevStudent.getApaarId(), apaarId);
                    putDiff(diffs, "dateOfBirth",
                            prevStudent.getDateOfBirth() != null ? prevStudent.getDateOfBirth().toString() : null,
                            payload.get("dateOfBirth"));
                    putDiff(diffs, "gender", prevStudent.getGender(), payload.get("gender"));

                    String email = (String) payload.get("email");
                    if (email != null && !email.equalsIgnoreCase(prevStudent.getEmail())) {
                        diffs.put("email", diff(prevStudent.getEmail(), email));
                    }
                    String phone = (String) payload.get("phone");
                    putDiff(diffs, "phone", prevStudent.getPhone(), phone);

                    putDiff(diffs, "guardianName", prevStudent.getGuardianName(), payload.get("guardianName"));
                    putDiff(diffs, "addressLine", prevStudent.getAddressLine(), payload.get("addressLine"));
                    putDiff(diffs, "state", prevStudent.getState(), payload.get("state"));
                }

                putDiff(diffs, "admissionCategory", prevEnrolment.getAdmissionCategory(), payload.get("admissionCategory"));
                putDiff(diffs, "quotaType", prevEnrolment.getQuotaType(), payload.get("quotaType"));
                putDiff(diffs, "admissionDate",
                        prevEnrolment.getAdmissionDate() != null ? prevEnrolment.getAdmissionDate().toString() : null,
                        payload.get("admissionDate"));

                if (diffs.isEmpty()) {
                    unchangedCount++;
                    items.add(new ReconciliationItemDto(
                            identifier,
                            apaarId,
                            fullName,
                            "UNCHANGED",
                            Collections.emptyMap()
                    ));
                } else {
                    updatedCount++;
                    items.add(new ReconciliationItemDto(
                            identifier,
                            apaarId,
                            fullName,
                            "DATA_UPDATE",
                            diffs
                    ));
                }
            }
        }

        // 3. Find missing from snapshot (active/provisional enrolments absent from current submission)
        int missingCount = 0;
        for (Map.Entry<String, Enrolment> entry : activeSnapshotMap.entrySet()) {
            if (!processedIdentifiers.contains(entry.getKey())) {
                missingCount++;
                Enrolment missingEnrolment = entry.getValue();
                Student s = prevStudentMap.get(missingEnrolment.getStudentId());
                items.add(new ReconciliationItemDto(
                        missingEnrolment.getEnrolmentNumber(),
                        s != null ? s.getApaarId() : null,
                        s != null ? s.getFullName() : "Unknown",
                        "MISSING_FROM_SNAPSHOT",
                        Map.of("status", missingEnrolment.getStatus(), "admissionCategory", missingEnrolment.getAdmissionCategory())
                ));
            }
        }

        return new ReconciliationSummaryDto(
                job.getId(),
                job.getInstitutionId(),
                job.getAcademicYearId(),
                job.getProgrammeId(),
                rows.size(),
                newCount,
                unchangedCount,
                updatedCount,
                missingCount,
                items
        );
    }

    private void putDiff(Map<String, Object> diffs, String field, Object oldValue, Object newValue) {
        if (newValue != null && !Objects.equals(oldValue, newValue)) {
            diffs.put(field, diff(oldValue, newValue));
        }
    }

    private Map<String, Object> diff(Object oldValue, Object newValue) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("old", oldValue);
        change.put("new", newValue);
        return change;
    }
}

