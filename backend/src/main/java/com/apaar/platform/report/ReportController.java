package com.apaar.platform.report;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reports & Exports", description = "Server-generated regulatory reports and CSV data exports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(value = "/enrolments.csv", produces = "text/csv")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    @Operation(summary = "Export student enrolments roster as CSV")
    public ResponseEntity<byte[]> downloadEnrolmentsCsv(
            @RequestParam(value = "institutionId", required = false) UUID institutionId,
            @RequestParam(value = "academicYearId", required = false) UUID academicYearId
    ) {
        byte[] csv = reportService.exportEnrolmentsCsv(institutionId, academicYearId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"enrolments.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}

