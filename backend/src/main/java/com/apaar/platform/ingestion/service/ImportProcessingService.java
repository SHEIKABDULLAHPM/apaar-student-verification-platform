package com.apaar.platform.ingestion.service;

import com.apaar.platform.academic.AcademicYearRepository;
import com.apaar.platform.academic.ProgrammeRepository;
import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.common.exception.ResourceNotFoundException;
import com.apaar.platform.common.exception.UnprocessableEntityException;
import com.apaar.platform.ingestion.*;
import com.apaar.platform.ingestion.dto.ImportDtos.*;
import com.apaar.platform.institution.InstitutionRepository;
import com.apaar.platform.student.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Service
public class ImportProcessingService {
    private static final Logger log = LoggerFactory.getLogger(ImportProcessingService.class);

    private final DataImportJobRepository jobRepository;
    private final DataImportRowRepository rowRepository;
    private final DataImportErrorRepository errorRepository;
    private final FileStorageService fileStorageService;
    private final SpreadsheetParserService spreadsheetParserService;
    private final FieldMappingService fieldMappingService;
    private final ValidationEngine validationEngine;
    private final ReconciliationService reconciliationService;
    private final StudentRepository studentRepository;
    private final EnrolmentRepository enrolmentRepository;
    private final StudentRecordVersionRepository recordVersionRepository;
    private final InstitutionRepository institutionRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ProgrammeRepository programmeRepository;

    public ImportProcessingService(
            DataImportJobRepository jobRepository,
            DataImportRowRepository rowRepository,
            DataImportErrorRepository errorRepository,
            FileStorageService fileStorageService,
            SpreadsheetParserService spreadsheetParserService,
            FieldMappingService fieldMappingService,
            ValidationEngine validationEngine,
            ReconciliationService reconciliationService,
            StudentRepository studentRepository,
            EnrolmentRepository enrolmentRepository,
            StudentRecordVersionRepository recordVersionRepository,
            InstitutionRepository institutionRepository,
            AcademicYearRepository academicYearRepository,
            ProgrammeRepository programmeRepository
    ) {
        this.jobRepository = jobRepository;
        this.rowRepository = rowRepository;
        this.errorRepository = errorRepository;
        this.fileStorageService = fileStorageService;
        this.spreadsheetParserService = spreadsheetParserService;
        this.fieldMappingService = fieldMappingService;
        this.validationEngine = validationEngine;
        this.reconciliationService = reconciliationService;
        this.studentRepository = studentRepository;
        this.enrolmentRepository = enrolmentRepository;
        this.recordVersionRepository = recordVersionRepository;
        this.institutionRepository = institutionRepository;
        this.academicYearRepository = academicYearRepository;
        this.programmeRepository = programmeRepository;
    }

    @Transactional
    public UploadResponseDto uploadFile(
            MultipartFile file,
            UUID institutionId,
            UUID academicYearId,
            UUID programmeId,
            UUID parentImportId,
            UUID uploadedBy
    ) {
        FileStorageService.StoredFile storedFile = fileStorageService.storeFile(file);

        SpreadsheetParserService.ParsedSpreadsheet parsed;
        try (InputStream is = fileStorageService.openStream(storedFile.storagePath())) {
            parsed = spreadsheetParserService.parse(is, storedFile.originalFilename());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read stored file for parsing", e);
        }

        if (parsed.headers().isEmpty()) {
            throw new UnprocessableEntityException("EMPTY_FILE", "The uploaded file does not contain any headers or records");
        }

        Map<String, String> suggestedMappings = fieldMappingService.suggestMappings(parsed.headers());
        List<String> unmappedHeaders = fieldMappingService.getUnmappedHeaders(parsed.headers(), suggestedMappings);
        List<String> missingMandatory = fieldMappingService.getMissingMandatoryFields(suggestedMappings);

        DataImportJob job = new DataImportJob();
        job.setInstitutionId(institutionId);
        job.setAcademicYearId(academicYearId);
        job.setProgrammeId(programmeId);
        job.setParentImportId(parentImportId);
        job.setOriginalFilename(storedFile.originalFilename());
        job.setContentType(storedFile.contentType());
        job.setFileChecksum(storedFile.checksum());
        job.setStoragePath(storedFile.storagePath());
        job.setStatus("RECEIVED");
        job.setTotalRows(parsed.rows().size());
        job.setFieldMappings(suggestedMappings);
        job.setUploadedBy(uploadedBy);

        DataImportJob savedJob = jobRepository.save(job);

        // Save raw rows
        List<DataImportRow> rows = new ArrayList<>();
        int rowNum = 1;
        for (Map<String, String> raw : parsed.rows()) {
            DataImportRow r = new DataImportRow(savedJob.getId(), rowNum++, raw);
            rows.add(r);
        }
        rowRepository.saveAll(rows);

        List<Map<String, String>> sampleRows = parsed.rows().stream().limit(5).toList();

        return new UploadResponseDto(
                savedJob.getId(),
                savedJob.getStatus(),
                savedJob.getOriginalFilename(),
                file.getSize(),
                parsed.rows().size(),
                parsed.headers(),
                suggestedMappings,
                unmappedHeaders,
                missingMandatory,
                sampleRows
        );
    }

    @Transactional
    public ImportJobSummaryDto applyMappingsAndValidate(UUID importJobId, Map<String, String> mappings) {
        DataImportJob job = jobRepository.findById(importJobId)
                .orElseThrow(() -> new ResourceNotFoundException("IMPORT_NOT_FOUND", "Import job not found: " + importJobId));

        List<String> missingMandatory = fieldMappingService.getMissingMandatoryFields(mappings);
        if (!missingMandatory.isEmpty()) {
            throw new UnprocessableEntityException("MANDATORY_MAPPINGS_MISSING",
                    "Mandatory canonical fields are not mapped: " + String.join(", ", missingMandatory));
        }

        job.setFieldMappings(mappings);
        job.setStatus("VALIDATING");
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        // Clear any previous errors
        errorRepository.deleteByImportJobId(importJobId);

        List<DataImportRow> rows = rowRepository.findByImportJobIdOrderByRowNumberAsc(importJobId);
        Set<String> seenIdentifiers = new HashSet<>();
        Set<String> seenApaar = new HashSet<>();

        int validCount = 0;
        int errorCount = 0;
        int duplicateCount = 0;
        int reviewCount = 0;

        List<DataImportError> allErrors = new ArrayList<>();

        for (DataImportRow row : rows) {
            Map<String, String> mappedRow = fieldMappingService.mapRowToCanonical(row.getRawData(), mappings);
            ValidationEngine.RowValidationResult res = validationEngine.validateRow(
                    importJobId,
                    row.getRowNumber(),
                    mappedRow,
                    job.getInstitutionId(),
                    job.getAcademicYearId(),
                    job.getProgrammeId(),
                    seenIdentifiers,
                    seenApaar
            );

            row.setNormalizedPayload(res.normalizedPayload());
            row.setRowStatus(res.status());
            row.setErrorCount(res.errors().size());
            row.setMatchedEnrolmentId(res.matchedEnrolmentId());
            row.setReconciliationAction(res.reconciliationAction());

            allErrors.addAll(res.errors());

            switch (res.status()) {
                case "VALID" -> validCount++;
                case "INVALID" -> errorCount++;
                case "DUPLICATE" -> duplicateCount++;
                case "REVIEW_REQUIRED" -> reviewCount++;
            }
        }

        rowRepository.saveAll(rows);
        if (!allErrors.isEmpty()) {
            errorRepository.saveAll(allErrors);
        }

        job.setValidRows(validCount);
        job.setErrorRows(errorCount);
        job.setDuplicateRows(duplicateCount);
        job.setReviewRows(reviewCount);

        // Compute reconciliation summary
        ReconciliationSummaryDto recon = reconciliationService.computeReconciliation(job);
        Map<String, Object> reconSummaryMap = Map.of(
                "totalCurrentSubmission", recon.totalCurrentSubmission(),
                "newAdmissions", recon.newAdmissions(),
                "unchangedCount", recon.unchangedCount(),
                "updatedCount", recon.updatedCount(),
                "missingFromSnapshotCount", recon.missingFromSnapshotCount()
        );
        job.setReconciliationSummary(reconSummaryMap);

        if (errorCount > 0 || duplicateCount > 0) {
            job.setStatus("VALIDATED_WITH_ERRORS");
        } else {
            job.setStatus("VALIDATED");
        }

        DataImportJob finalJob = jobRepository.save(job);
        return mapToSummaryDto(finalJob);
    }

    @Transactional(readOnly = true)
    public ImportPreviewDto getPreview(UUID importJobId, String statusFilter, Pageable pageable) {
        DataImportJob job = jobRepository.findById(importJobId)
                .orElseThrow(() -> new ResourceNotFoundException("IMPORT_NOT_FOUND", "Import job not found: " + importJobId));

        Page<DataImportRow> rowsPage;
        if (statusFilter != null && !statusFilter.isBlank()) {
            rowsPage = rowRepository.findByImportJobIdAndRowStatusOrderByRowNumberAsc(importJobId, statusFilter.toUpperCase(), pageable);
        } else {
            rowsPage = rowRepository.findByImportJobIdOrderByRowNumberAsc(importJobId, pageable);
        }

        Map<Integer, List<RowErrorDto>> errorsByRow = new HashMap<>();
        List<DataImportError> errors = errorRepository.findByImportJobIdOrderByRowNumberAsc(importJobId);
        for (DataImportError err : errors) {
            errorsByRow.computeIfAbsent(err.getRowNumber(), k -> new ArrayList<>())
                    .add(new RowErrorDto(err.getRowNumber(), err.getColumnName(), err.getErrorCode(), err.getMessage(), err.getRawValue()));
        }

        List<NormalizedRowDto> rowDtos = rowsPage.getContent().stream().map(r -> new NormalizedRowDto(
                r.getId(),
                r.getRowNumber(),
                r.getRowStatus(),
                r.getReconciliationAction(),
                r.getErrorCount(),
                r.getNormalizedPayload(),
                r.getRawData(),
                errorsByRow.getOrDefault(r.getRowNumber(), Collections.emptyList())
        )).toList();

        return new ImportPreviewDto(
                mapToSummaryDto(job),
                job.getTotalRows(),
                job.getValidRows(),
                job.getErrorRows(),
                job.getDuplicateRows(),
                job.getReviewRows(),
                rowDtos
        );
    }

    @Transactional
    public CommitResponseDto commitImport(UUID importJobId, UUID userId) {
        DataImportJob job = jobRepository.findById(importJobId)
                .orElseThrow(() -> new ResourceNotFoundException("IMPORT_NOT_FOUND", "Import job not found: " + importJobId));

        if ("COMPLETED".equals(job.getStatus()) || "COMPLETED_WITH_ERRORS".equals(job.getStatus())) {
            log.info("Import job {} has already been committed with status {}. Returning existing commit summary.", importJobId, job.getStatus());
            return new CommitResponseDto(
                    job.getId(),
                    job.getStatus(),
                    job.getInsertedRows(),
                    job.getUpdatedRows(),
                    job.getInsertedRows() + job.getUpdatedRows(),
                    job.getCompletedAt() != null ? job.getCompletedAt() : Instant.now()
            );
        }

        if (!"VALIDATED".equals(job.getStatus()) && !"VALIDATED_WITH_ERRORS".equals(job.getStatus())) {
            throw new UnprocessableEntityException("ILLEGAL_STATUS", "Only validated imports can be committed. Current status: " + job.getStatus());
        }

        List<DataImportRow> rows = rowRepository.findByImportJobIdOrderByRowNumberAsc(importJobId);
        int inserted = 0;
        int updated = 0;

        for (DataImportRow row : rows) {
            // Only commit VALID rows (or REVIEW_REQUIRED if accepted)
            if (!"VALID".equals(row.getRowStatus()) && !"REVIEW_REQUIRED".equals(row.getRowStatus())) {
                continue;
            }

            Map<String, Object> payload = row.getNormalizedPayload();
            if (payload == null) continue;

            String identifier = (String) payload.get("studentIdentifier");
            String fullName = (String) payload.get("fullName");
            String apaarId = (String) payload.get("apaarId");
            Boolean apaarValid = (Boolean) payload.getOrDefault("apaarFormatValid", false);
            String rawDob = (String) payload.get("dateOfBirth");
            LocalDate dob = rawDob != null ? LocalDate.parse(rawDob) : null;
            String gender = (String) payload.get("gender");
            String category = (String) payload.get("admissionCategory");
            String rawAdmDate = (String) payload.get("admissionDate");
            LocalDate admDate = rawAdmDate != null ? LocalDate.parse(rawAdmDate) : LocalDate.now();
            String email = (String) payload.get("email");
            String phone = (String) payload.get("phone");
            String guardianName = (String) payload.get("guardianName");
            String addressLine = (String) payload.get("addressLine");
            String state = (String) payload.get("state");
            String quotaType = (String) payload.get("quotaType");

            // Look up existing enrolment by institution and enrolment number
            Optional<Enrolment> existingEnrolmentOpt = enrolmentRepository.findByInstitutionIdAndEnrolmentNumber(
                    job.getInstitutionId(), identifier);

            Student student;

            if (existingEnrolmentOpt.isPresent()) {
                Enrolment enr = existingEnrolmentOpt.get();
                student = studentRepository.findById(enr.getStudentId())
                        .orElseThrow(() -> new IllegalStateException("Student missing for enrolment: " + enr.getId()));

                Optional<Student> studentByApaar = (apaarId != null && !apaarId.isBlank()) ?
                        studentRepository.findByApaarId(apaarId) : Optional.empty();

                if (studentByApaar.isPresent() && !studentByApaar.get().getId().equals(student.getId())) {
                    // APAAR already belongs to another student; reconcile enrolment to target student
                    Student targetStudent = studentByApaar.get();

                    // Withdraw and immediately flush any other active/provisional enrolments for targetStudent in this programme & year
                    List<Enrolment> existingTargetEnrs = enrolmentRepository.findByStudentIdAndAcademicYearIdAndStatusIn(
                            targetStudent.getId(), enr.getAcademicYearId(), List.of("ACTIVE", "PROVISIONAL"));
                    for (Enrolment other : existingTargetEnrs) {
                        if (!other.getId().equals(enr.getId()) && other.getProgrammeId().equals(enr.getProgrammeId())) {
                            other.setStatus("WITHDRAWN");
                            enrolmentRepository.saveAndFlush(other);
                        }
                    }

                    // Release old student's lock on APAAR if any
                    student.setApaarId(null);
                    studentRepository.saveAndFlush(student);

                    enr.setStudentId(targetStudent.getId());
                    updateStudentFields(targetStudent, fullName, targetStudent.getApaarId(), targetStudent.isApaarFormatValid(),
                            dob, gender, category, email, phone, guardianName, addressLine, state);
                    studentRepository.saveAndFlush(targetStudent);
                    recordVersion(targetStudent, "IMPORT_RECONCILE: Job " + job.getId(), userId);
                } else {
                    // Update student details
                    updateStudentFields(student, fullName, apaarId, apaarValid, dob, gender, category, email, phone, guardianName, addressLine, state);
                    studentRepository.saveAndFlush(student);
                    recordVersion(student, "IMPORT_UPDATE: Job " + job.getId(), userId);
                }

                // Update enrolment
                enr.setAdmissionCategory(category);
                enr.setQuotaType(quotaType);
                enr.setAdmissionDate(admDate);
                enr.setSourceImportId(job.getId());
                enrolmentRepository.saveAndFlush(enr);
                updated++;
            } else {
                // Check if student exists by APAAR
                Optional<Student> studentByApaar = (apaarId != null && !apaarId.isBlank()) ?
                        studentRepository.findByApaarId(apaarId) : Optional.empty();

                if (studentByApaar.isPresent()) {
                    student = studentByApaar.get();
                    updateStudentFields(student, fullName, apaarId, apaarValid, dob, gender, category, email, phone, guardianName, addressLine, state);
                    studentRepository.save(student);
                    recordVersion(student, "IMPORT_UPDATE: Job " + job.getId(), userId);
                } else {
                    student = new Student();
                    student.setFullName(fullName);
                    student.setApaarId(apaarId);
                    student.setApaarFormatValid(apaarValid);
                    if (apaarValid) student.setApaarFormatValidatedAt(Instant.now());
                    student.setDateOfBirth(dob);
                    student.setGender(gender);
                    student.setCategory(category);
                    student.setEmail(email);
                    student.setPhone(phone);
                    student.setGuardianName(guardianName);
                    student.setAddressLine(addressLine);
                    student.setState(state);
                    student.setCreatedBy(userId);
                    student = studentRepository.save(student);

                    recordVersion(student, "INITIAL_IMPORT: Job " + job.getId(), userId);
                }

                UUID targetProgrammeId = job.getProgrammeId();
                if (targetProgrammeId == null) {
                    String progCode = (String) payload.get("programmeCode");
                    if (progCode != null && !progCode.isBlank()) {
                        targetProgrammeId = programmeRepository.findByInstitutionIdAndCodeIgnoreCase(job.getInstitutionId(), progCode.trim())
                                .map(p -> p.getId()).orElse(null);
                    }
                    if (targetProgrammeId == null) {
                        List<com.apaar.platform.academic.Programme> instProgrammes = programmeRepository.findByInstitutionIdOrderByName(job.getInstitutionId());
                        if (!instProgrammes.isEmpty()) {
                            targetProgrammeId = instProgrammes.get(0).getId();
                        } else {
                            throw new UnprocessableEntityException("NO_PROGRAMME_CONFIGURED",
                                    "No programme found for this institution. Please configure an approved programme first.");
                        }
                    }
                }

                List<Enrolment> existingStudentEnrs = enrolmentRepository.findByStudentIdAndAcademicYearIdAndStatusIn(
                        student.getId(), job.getAcademicYearId(), List.of("ACTIVE", "PROVISIONAL"));

                final UUID finalTargetProgId = targetProgrammeId;
                Optional<Enrolment> existingProgEnr = existingStudentEnrs.stream()
                        .filter(e -> e.getProgrammeId().equals(finalTargetProgId))
                        .findFirst();

                if (existingProgEnr.isPresent()) {
                    Enrolment existing = existingProgEnr.get();
                    for (Enrolment other : existingStudentEnrs) {
                        if (!other.getId().equals(existing.getId()) && other.getProgrammeId().equals(finalTargetProgId)) {
                            other.setStatus("WITHDRAWN");
                            enrolmentRepository.saveAndFlush(other);
                        }
                    }
                    existing.setEnrolmentNumber(identifier);
                    existing.setAdmissionCategory(category);
                    existing.setQuotaType(quotaType);
                    existing.setAdmissionDate(admDate);
                    existing.setSourceImportId(job.getId());
                    enrolmentRepository.saveAndFlush(existing);
                    updated++;
                } else {
                    // Create Enrolment
                    Enrolment enrolment = new Enrolment();
                    enrolment.setStudentId(student.getId());
                    enrolment.setInstitutionId(job.getInstitutionId());
                    enrolment.setProgrammeId(targetProgrammeId);
                    enrolment.setAcademicYearId(job.getAcademicYearId());
                    enrolment.setEnrolmentNumber(identifier);
                    enrolment.setAdmissionCategory(category);
                    enrolment.setQuotaType(quotaType);
                    enrolment.setAdmissionDate(admDate);
                    enrolment.setStatus("ACTIVE");
                    enrolment.setSourceImportId(job.getId());
                    enrolment.setCreatedBy(userId);
                    enrolmentRepository.saveAndFlush(enrolment);
                    inserted++;
                }
            }
        }

        job.setInsertedRows(inserted);
        job.setUpdatedRows(updated);
        job.setStatus(job.getErrorRows() > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
        job.setCompletedAt(Instant.now());
        jobRepository.save(job);

        return new CommitResponseDto(
                job.getId(),
                job.getStatus(),
                inserted,
                updated,
                inserted + updated,
                job.getCompletedAt()
        );
    }

    private void updateStudentFields(
            Student s, String fullName, String apaarId, Boolean apaarValid,
            LocalDate dob, String gender, String category, String email, String phone,
            String guardianName, String addressLine, String state
    ) {
        if (fullName != null && !fullName.isBlank()) s.setFullName(fullName);
        if (apaarId != null && !apaarId.isBlank()) {
            s.setApaarId(apaarId);
            s.setApaarFormatValid(apaarValid);
            s.setApaarFormatValidatedAt(Instant.now());
        }
        if (dob != null) s.setDateOfBirth(dob);
        if (gender != null && !gender.isBlank()) s.setGender(gender);
        if (category != null && !category.isBlank()) s.setCategory(category);
        if (email != null && !email.isBlank()) s.setEmail(email);
        if (phone != null && !phone.isBlank()) s.setPhone(phone);
        if (guardianName != null && !guardianName.isBlank()) s.setGuardianName(guardianName);
        if (addressLine != null && !addressLine.isBlank()) s.setAddressLine(addressLine);
        if (state != null && !state.isBlank()) s.setState(state);
    }

    private void recordVersion(Student student, String reason, UUID changedBy) {
        int latestVersion = recordVersionRepository.findLatestVersionNo(student.getId());
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", student.getId());
        snapshot.put("fullName", student.getFullName());
        snapshot.put("apaarId", student.getApaarId());
        snapshot.put("apaarFormatValid", student.isApaarFormatValid());
        snapshot.put("dateOfBirth", student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : null);
        snapshot.put("gender", student.getGender());
        snapshot.put("category", student.getCategory());
        snapshot.put("email", student.getEmail());
        snapshot.put("phone", student.getPhone());
        snapshot.put("timestamp", Instant.now().toString());

        StudentRecordVersion version = new StudentRecordVersion(
                student.getId(),
                latestVersion + 1,
                snapshot,
                reason,
                changedBy
        );
        recordVersionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public ImportJobSummaryDto getJobSummary(UUID importJobId) {
        DataImportJob job = jobRepository.findById(importJobId)
                .orElseThrow(() -> new ResourceNotFoundException("IMPORT_NOT_FOUND", "Import job not found: " + importJobId));
        return mapToSummaryDto(job);
    }

    @Transactional(readOnly = true)
    public PageResponse<ImportJobSummaryDto> searchJobs(UUID institutionId, UUID academicYearId, String status, Pageable pageable) {
        Page<DataImportJob> page = jobRepository.search(institutionId, academicYearId, status, pageable);
        List<ImportJobSummaryDto> dtoList = page.getContent().stream().map(this::mapToSummaryDto).toList();
        return PageResponse.of(dtoList, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResponse<RowErrorDto> getJobErrors(UUID importJobId, Pageable pageable) {
        Page<DataImportError> page = errorRepository.findByImportJobIdOrderByRowNumberAsc(importJobId, pageable);
        List<RowErrorDto> dtos = page.getContent().stream().map(e -> new RowErrorDto(
                e.getRowNumber(),
                e.getColumnName(),
                e.getErrorCode(),
                e.getMessage(),
                e.getRawValue()
        )).toList();
        return PageResponse.of(dtos, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    private ImportJobSummaryDto mapToSummaryDto(DataImportJob job) {
        String instName = institutionRepository.findById(job.getInstitutionId())
                .map(i -> i.getName()).orElse("Unknown Institution");
        String yearCode = academicYearRepository.findById(job.getAcademicYearId())
                .map(y -> y.getCode()).orElse("Unknown Year");
        String progName = job.getProgrammeId() != null ?
                programmeRepository.findById(job.getProgrammeId()).map(p -> p.getName()).orElse(null) : null;

        return new ImportJobSummaryDto(
                job.getId(),
                job.getInstitutionId(),
                instName,
                job.getAcademicYearId(),
                yearCode,
                job.getProgrammeId(),
                progName,
                job.getParentImportId(),
                job.getOriginalFilename(),
                job.getContentType(),
                job.getStatus(),
                job.getTotalRows(),
                job.getValidRows(),
                job.getErrorRows(),
                job.getDuplicateRows(),
                job.getReviewRows(),
                job.getInsertedRows(),
                job.getUpdatedRows(),
                job.getFieldMappings(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }
}
