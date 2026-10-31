package com.apaar.platform.ingestion.service;

import com.apaar.platform.ingestion.DataImportError;
import com.apaar.platform.ingestion.DataImportErrorRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
public class ErrorReportService {

    private final DataImportErrorRepository errorRepository;

    public ErrorReportService(DataImportErrorRepository errorRepository) {
        this.errorRepository = errorRepository;
    }

    public byte[] generateCsvReport(UUID importJobId) {
        List<DataImportError> errors = errorRepository.findByImportJobIdOrderByRowNumberAsc(importJobId);
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.builder()
                .setHeader("Row Number", "Column", "Error Code", "Error Message", "Raw Value")
                .build())) {

            for (DataImportError err : errors) {
                printer.printRecord(
                        err.getRowNumber(),
                        sanitizeFormulaInjection(err.getColumnName()),
                        sanitizeFormulaInjection(err.getErrorCode()),
                        sanitizeFormulaInjection(err.getMessage()),
                        sanitizeFormulaInjection(err.getRawValue())
                );
            }
            printer.flush();
            return sw.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV error report", e);
        }
    }

    public byte[] generateExcelReport(UUID importJobId) {
        List<DataImportError> errors = errorRepository.findByImportJobIdOrderByRowNumberAsc(importJobId);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Import Errors");

            // Header styling
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Row Number", "Column", "Error Code", "Error Message", "Raw Value"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (DataImportError err : errors) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(err.getRowNumber());
                row.createCell(1).setCellValue(sanitizeFormulaInjection(err.getColumnName()));
                row.createCell(2).setCellValue(sanitizeFormulaInjection(err.getErrorCode()));
                row.createCell(3).setCellValue(sanitizeFormulaInjection(err.getMessage()));
                row.createCell(4).setCellValue(sanitizeFormulaInjection(err.getRawValue()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel error report", e);
        }
    }

    /**
     * Prevents CSV/Excel formula injection by prepending a single quote if a cell starts with =, +, -, @, or tab/return.
     */
    public static String sanitizeFormulaInjection(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return "";
        char first = trimmed.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
            return "'" + value;
        }
        return value;
    }
}

