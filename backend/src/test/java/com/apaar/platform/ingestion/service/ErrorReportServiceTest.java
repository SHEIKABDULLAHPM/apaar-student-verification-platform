package com.apaar.platform.ingestion.service;

import com.apaar.platform.ingestion.DataImportError;
import com.apaar.platform.ingestion.DataImportErrorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorReportServiceTest {

    @Mock
    private DataImportErrorRepository errorRepository;

    @InjectMocks
    private ErrorReportService errorReportService;

    @Test
    @DisplayName("Formula injection strings starting with =, +, -, @ are escaped with leading single quote")
    void testSanitizeFormulaInjection() {
        assertThat(ErrorReportService.sanitizeFormulaInjection("=SUM(1+1)")).isEqualTo("'=SUM(1+1)");
        assertThat(ErrorReportService.sanitizeFormulaInjection("+123456")).isEqualTo("'+123456");
        assertThat(ErrorReportService.sanitizeFormulaInjection("-100")).isEqualTo("'-100");
        assertThat(ErrorReportService.sanitizeFormulaInjection("@cmd")).isEqualTo("'@cmd");
        assertThat(ErrorReportService.sanitizeFormulaInjection("RegularText")).isEqualTo("RegularText");
        assertThat(ErrorReportService.sanitizeFormulaInjection(null)).isEqualTo("");
    }

    @Test
    @DisplayName("Generates CSV error report safely")
    void testGenerateCsvReport() {
        UUID jobId = UUID.randomUUID();
        List<DataImportError> errors = List.of(
                new DataImportError(jobId, 2, "apaarId", "INVALID_APAAR_FORMAT", "Expected 12 digits", "=DANGEROUS()")
        );
        when(errorRepository.findByImportJobIdOrderByRowNumberAsc(jobId)).thenReturn(errors);

        byte[] csv = errorReportService.generateCsvReport(jobId);
        String text = new String(csv, StandardCharsets.UTF_8);

        assertThat(text).contains("Row Number");
        assertThat(text).contains("INVALID_APAAR_FORMAT");
        assertThat(text).contains("'=DANGEROUS()");
    }

    @Test
    @DisplayName("Generates Excel error report bytes")
    void testGenerateExcelReport() {
        UUID jobId = UUID.randomUUID();
        List<DataImportError> errors = List.of(
                new DataImportError(jobId, 5, "email", "INVALID_EMAIL", "Malformed email address", "bad-email")
        );
        when(errorRepository.findByImportJobIdOrderByRowNumberAsc(jobId)).thenReturn(errors);

        byte[] excel = errorReportService.generateExcelReport(jobId);
        assertThat(excel).isNotEmpty();
        // PK zip header bytes for .xlsx
        assertThat(excel[0]).isEqualTo((byte) 0x50);
        assertThat(excel[1]).isEqualTo((byte) 0x4B);
    }
}

