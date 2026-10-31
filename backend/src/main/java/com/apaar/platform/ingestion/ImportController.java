package com.apaar.platform.ingestion;

import com.apaar.platform.common.api.PageResponse;
import com.apaar.platform.ingestion.dto.ImportDtos.*;
import com.apaar.platform.ingestion.service.ErrorReportService;
import com.apaar.platform.ingestion.service.ImportProcessingService;
import com.apaar.platform.ingestion.service.ReconciliationService;
import com.apaar.platform.security.AccessScopeService;
import com.apaar.platform.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/imports")
@Tag(name = "Data Ingestion & Imports", description = "Bulk student data uploads, mapping, validation, error reports, and reconciliation")
public class ImportController {

    private final ImportProcessingService importProcessingService;
    private final ReconciliationService reconciliationService;
    private final ErrorReportService errorReportService;
    private final AccessScopeService accessScopeService;
    private final DataImportJobRepository jobRepository;

    public ImportController(
            ImportProcessingService importProcessingService,
            ReconciliationService reconciliationService,
            ErrorReportService errorReportService,
            AccessScopeService accessScopeService,
            DataImportJobRepository jobRepository
    ) {
        this.importProcessingService = importProcessingService;
        this.reconciliationService = reconciliationService;
        this.errorReportService = errorReportService;
        this.accessScopeService = accessScopeService;
        this.jobRepository = jobRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('IMPORT_WRITE')")
    @Operation(summary = "Upload CSV or Excel file for student ingestion")
    public UploadResponseDto upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "institutionId", required = false) UUID institutionId,
            @RequestParam("academicYearId") UUID academicYearId,
            @RequestParam(value = "programmeId", required = false) UUID programmeId,
            @RequestParam(value = "parentImportId", required = false) UUID parentImportId
    ) {
        UUID effectiveInstId = accessScopeService.requireWritableInstitution(institutionId);
        UserPrincipal principal = accessScopeService.requirePrincipal();

        return importProcessingService.uploadFile(
                file,
                effectiveInstId,
                academicYearId,
                programmeId,
                parentImportId,
                principal.getUserId()
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('IMPORT_READ')")
    @Operation(summary = "Search import jobs scoped to institution")
    public PageResponse<ImportJobSummaryDto> list(
            @RequestParam(value = "institutionId", required = false) UUID institutionId,
            @RequestParam(value = "academicYearId", required = false) UUID academicYearId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        UUID effectiveInstId = accessScopeService.resolveInstitutionFilter(institutionId);
        Pageable pageable = PageRequest.of(page, size);
        return importProcessingService.searchJobs(effectiveInstId, academicYearId, status, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('IMPORT_READ')")
    @Operation(summary = "Get import job summary")
    public ImportJobSummaryDto getJob(@PathVariable("id") UUID id) {
        ImportJobSummaryDto summary = importProcessingService.getJobSummary(id);
        accessScopeService.assertInstitutionAccess(summary.institutionId());
        return summary;
    }

    @PostMapping("/{id}/mappings")
    @PreAuthorize("hasAuthority('IMPORT_WRITE')")
    @Operation(summary = "Apply column mappings and trigger row validation")
    public ImportJobSummaryDto applyMappings(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ApplyMappingsRequest request
    ) {
        ImportJobSummaryDto existing = importProcessingService.getJobSummary(id);
        accessScopeService.assertInstitutionAccess(existing.institutionId());
        return importProcessingService.applyMappingsAndValidate(id, request.mappings());
    }

    @GetMapping("/{id}/preview")
    @PreAuthorize("hasAuthority('IMPORT_READ')")
    @Operation(summary = "Preview parsed rows with status filter and pagination")
    public ImportPreviewDto preview(
            @PathVariable("id") UUID id,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        ImportJobSummaryDto existing = importProcessingService.getJobSummary(id);
        accessScopeService.assertInstitutionAccess(existing.institutionId());
        return importProcessingService.getPreview(id, status, PageRequest.of(page, size));
    }

    @GetMapping("/{id}/errors")
    @PreAuthorize("hasAuthority('IMPORT_READ')")
    @Operation(summary = "Get paginated errors for an import job")
    public PageResponse<RowErrorDto> getErrors(
            @PathVariable("id") UUID id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "50") int size
    ) {
        ImportJobSummaryDto existing = importProcessingService.getJobSummary(id);
        accessScopeService.assertInstitutionAccess(existing.institutionId());
        return importProcessingService.getJobErrors(id, PageRequest.of(page, size));
    }

    @GetMapping("/{id}/error-report")
    @PreAuthorize("hasAuthority('IMPORT_READ')")
    @Operation(summary = "Download error report as CSV or Excel with formula injection protection")
    public ResponseEntity<byte[]> downloadErrorReport(
            @PathVariable("id") UUID id,
            @RequestParam(value = "format", defaultValue = "csv") String format
    ) {
        ImportJobSummaryDto existing = importProcessingService.getJobSummary(id);
        accessScopeService.assertInstitutionAccess(existing.institutionId());

        boolean isExcel = "xlsx".equalsIgnoreCase(format) || "excel".equalsIgnoreCase(format);
        byte[] data = isExcel ? errorReportService.generateExcelReport(id) : errorReportService.generateCsvReport(id);
        String filename = "import_errors_" + id + (isExcel ? ".xlsx" : ".csv");
        String contentType = isExcel ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : "text/csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(data);
    }

    @PostMapping("/{id}/commit")
    @PreAuthorize("hasAuthority('IMPORT_WRITE')")
    @Operation(summary = "Commit valid rows into student registry and enrolments")
    public CommitResponseDto commit(@PathVariable("id") UUID id) {
        ImportJobSummaryDto existing = importProcessingService.getJobSummary(id);
        accessScopeService.assertInstitutionAccess(existing.institutionId());
        UserPrincipal principal = accessScopeService.requirePrincipal();
        return importProcessingService.commitImport(id, principal.getUserId());
    }

    @GetMapping("/{id}/reconciliation")
    @PreAuthorize("hasAuthority('IMPORT_READ')")
    @Operation(summary = "Get reconciliation comparison against existing snapshot")
    public ReconciliationSummaryDto getReconciliation(@PathVariable("id") UUID id) {
        DataImportJob job = jobRepository.findById(id)
                .orElseThrow(() -> new com.apaar.platform.common.exception.ResourceNotFoundException("IMPORT_NOT_FOUND", "Import not found: " + id));
        accessScopeService.assertInstitutionAccess(job.getInstitutionId());
        return reconciliationService.computeReconciliation(job);
    }

    @PostMapping(value = "/{id}/resubmissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('IMPORT_WRITE')")
    @Operation(summary = "Upload corrected file linked to parent import job")
    public UploadResponseDto resubmit(
            @PathVariable("id") UUID id,
            @RequestParam("file") MultipartFile file
    ) {
        ImportJobSummaryDto parent = importProcessingService.getJobSummary(id);
        accessScopeService.assertInstitutionAccess(parent.institutionId());
        UserPrincipal principal = accessScopeService.requirePrincipal();

        return importProcessingService.uploadFile(
                file,
                parent.institutionId(),
                parent.academicYearId(),
                parent.programmeId(),
                parent.id(),
                principal.getUserId()
        );
    }
}

