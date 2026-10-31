package com.apaar.platform.ingestion.service;

import com.apaar.platform.common.exception.UnprocessableEntityException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SpreadsheetParserService {

    public record ParsedSpreadsheet(
            List<String> headers,
            List<Map<String, String>> rows,
            int totalRows
    ) {}

    public ParsedSpreadsheet parse(InputStream inputStream, String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".csv")) {
            return parseCsv(inputStream);
        } else if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return parseExcel(inputStream);
        } else {
            throw new UnprocessableEntityException("UNSUPPORTED_FILE_TYPE", "File must be a CSV (.csv) or Excel spreadsheet (.xlsx, .xls)");
        }
    }

    private ParsedSpreadsheet parseCsv(InputStream is) {
        try {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .setAllowMissingColumnNames(true)
                    .build();

            try (CSVParser parser = new CSVParser(new InputStreamReader(is, StandardCharsets.UTF_8), format)) {
                List<String> rawHeaders = new ArrayList<>(parser.getHeaderNames());
                if (rawHeaders.isEmpty()) {
                    throw new UnprocessableEntityException("EMPTY_HEADER", "CSV file does not contain a valid header row");
                }

                // Strip a UTF-8 BOM from the first header; the parser still keys records by the raw name.
                List<String> headers = new ArrayList<>(rawHeaders.size());
                for (int i = 0; i < rawHeaders.size(); i++) {
                    String header = rawHeaders.get(i);
                    if (i == 0 && header != null) {
                        header = stripBom(header);
                    }
                    headers.add(header);
                }

                List<Map<String, String>> rows = new ArrayList<>();
                for (CSVRecord record : parser) {
                    Map<String, String> rowMap = new LinkedHashMap<>();
                    boolean hasContent = false;
                    for (int i = 0; i < rawHeaders.size(); i++) {
                        String rawHeader = rawHeaders.get(i);
                        String header = headers.get(i);
                        String val = record.isMapped(rawHeader) ? record.get(rawHeader) : "";
                        if (val != null) {
                            val = val.trim();
                            if (!val.isEmpty()) hasContent = true;
                        } else {
                            val = "";
                        }
                        rowMap.put(header, val);
                    }
                    if (hasContent) {
                        rows.add(rowMap);
                    }
                }
                return new ParsedSpreadsheet(headers, rows, rows.size());
            }
        } catch (UnprocessableEntityException uee) {
            throw uee;
        } catch (Exception e) {
            throw new UnprocessableEntityException("CSV_PARSE_ERROR", "Failed to parse CSV file: " + e.getMessage());
        }
    }

    private ParsedSpreadsheet parseExcel(InputStream is) {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new UnprocessableEntityException("EMPTY_EXCEL", "Excel workbook contains no sheets");
            }
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() == 0) {
                throw new UnprocessableEntityException("EMPTY_SHEET", "First sheet in workbook contains no rows");
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                throw new UnprocessableEntityException("EMPTY_HEADER", "Excel sheet contains no header row");
            }

            DataFormatter dataFormatter = new DataFormatter();
            Row headerRow = rowIterator.next();
            List<String> headers = new ArrayList<>();
            int lastCellNum = headerRow.getLastCellNum();
            for (int i = 0; i < lastCellNum; i++) {
                Cell cell = headerRow.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                String header = (cell != null) ? formatCellValue(cell, dataFormatter).trim() : "";
                if (header.isEmpty()) {
                    header = "Column_" + (i + 1);
                }
                headers.add(header);
            }
            List<Map<String, String>> rows = new ArrayList<>();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Map<String, String> rowMap = new LinkedHashMap<>();
                boolean hasContent = false;

                for (int i = 0; i < headers.size(); i++) {
                    String header = headers.get(i);
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String val = "";
                    if (cell != null) {
                        val = formatCellValue(cell, dataFormatter).trim();
                        if (!val.isEmpty()) hasContent = true;
                    }
                    rowMap.put(header, val);
                }

                if (hasContent) {
                    rows.add(rowMap);
                }
            }

            return new ParsedSpreadsheet(headers, rows, rows.size());
        } catch (UnprocessableEntityException uee) {
            throw uee;
        } catch (Exception e) {
            throw new UnprocessableEntityException("EXCEL_PARSE_ERROR", "Failed to parse Excel file: " + e.getMessage());
        }
    }

    private String formatCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        CellType cellType = cell.getCellType();

        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }

        switch (cellType) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return new SimpleDateFormat("yyyy-MM-dd").format(date);
                } else {
                    double num = cell.getNumericCellValue();
                    if (num == Math.floor(num) && !Double.isInfinite(num)) {
                        return String.valueOf((long) num);
                    }
                    return formatter.formatCellValue(cell);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return formatter.formatCellValue(cell);
        }
    }

    private String stripBom(String value) {
        return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
    }
}

