package com.apaar.platform.ingestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ImportDtos {

    public record ImportJobSummaryDto(
            UUID id,
            UUID institutionId,
            String institutionName,
            UUID academicYearId,
            String academicYearCode,
            UUID programmeId,
            String programmeName,
            UUID parentImportId,
            String originalFilename,
            String contentType,
            String status,
            int totalRows,
            int validRows,
            int errorRows,
            int duplicateRows,
            int reviewRows,
            int insertedRows,
            int updatedRows,
            Map<String, String> fieldMappings,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt
    ) {}

    public record UploadResponseDto(
            UUID importId,
            String status,
            String filename,
            long fileSize,
            int totalRowsDetected,
            List<String> sourceHeaders,
            Map<String, String> suggestedMappings,
            List<String> unmappedHeaders,
            List<String> missingMandatoryFields,
            List<Map<String, String>> sampleRows
    ) {}

    public record ApplyMappingsRequest(
            @NotNull Map<String, String> mappings
    ) {}

    public record RowErrorDto(
            int rowNumber,
            String columnName,
            String errorCode,
            String message,
            String rawValue
    ) {}

    public record NormalizedRowDto(
            UUID id,
            int rowNumber,
            String rowStatus,
            String reconciliationAction,
            int errorCount,
            Map<String, Object> payload,
            Map<String, String> rawData,
            List<RowErrorDto> errors
    ) {}

    public record ImportPreviewDto(
            ImportJobSummaryDto job,
            int totalRows,
            int validRows,
            int errorRows,
            int duplicateRows,
            int reviewRows,
            List<NormalizedRowDto> rows
    ) {}

    public record CommitResponseDto(
            UUID importId,
            String status,
            int insertedRows,
            int updatedRows,
            int totalCommitted,
            Instant completedAt
    ) {}

    public record ReconciliationItemDto(
            String studentIdentifier,
            String apaarId,
            String studentName,
            String action, // NEW, UNCHANGED, UPDATED, MISSING_FROM_SNAPSHOT, CONFLICT
            Map<String, Object> changedFields
    ) {}

    public record ReconciliationSummaryDto(
            UUID importJobId,
            UUID institutionId,
            UUID academicYearId,
            UUID programmeId,
            int totalCurrentSubmission,
            int newAdmissions,
            int unchangedCount,
            int updatedCount,
            int missingFromSnapshotCount,
            List<ReconciliationItemDto> items
    ) {}

    public record SingleSubmissionRequest(
            UUID institutionId,
            @NotNull UUID academicYearId,
            @NotNull UUID programmeId,
            @NotBlank String studentIdentifier,
            String apaarId,
            @NotBlank String fullName,
            LocalDate dateOfBirth,
            String gender,
            @NotBlank String admissionCategory,
            LocalDate admissionDate,
            String email,
            String phone,
            String guardianName,
            String addressLine,
            String state,
            String quotaType,
            Boolean supernumerary,
            Boolean nriClassification,
            Boolean ociClassification,
            Boolean foreignNationalClassification
    ) {}

    public record SingleSubmissionResult(
            boolean success,
            String studentIdentifier,
            UUID studentId,
            UUID enrolmentId,
            String apaarStatus,
            List<RowErrorDto> errors
    ) {}

    public record BatchSubmissionRequest(
            UUID institutionId,
            @NotNull UUID academicYearId,
            @NotNull UUID programmeId,
            @NotNull List<SingleSubmissionRequest> records
    ) {}

    public record BatchSubmissionResult(
            int totalSubmitted,
            int successCount,
            int failureCount,
            List<SingleSubmissionResult> results
    ) {}
}

