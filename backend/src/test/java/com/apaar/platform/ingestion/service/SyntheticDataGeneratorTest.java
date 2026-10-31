package com.apaar.platform.ingestion.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class SyntheticDataGeneratorTest {

    @Test
    void generateExcelFilesFromCsv() throws Exception {
        Path demoDir = Paths.get("../database/synthetic-demo-data").toAbsolutePath().normalize();
        if (!Files.exists(demoDir)) {
            demoDir = Paths.get("database/synthetic-demo-data").toAbsolutePath().normalize();
        }

        if (Files.exists(demoDir)) {
            convertCsvToExcel(demoDir.resolve("initial_student_intake_valid.csv"), demoDir.resolve("initial_student_intake_valid.xlsx"));
            convertCsvToExcel(demoDir.resolve("initial_student_intake_mixed_scenarios.csv"), demoDir.resolve("initial_student_intake_mixed_scenarios.xlsx"));
        }
    }

    private void convertCsvToExcel(Path csvPath, Path xlsxPath) throws Exception {
        if (!Files.exists(csvPath)) return;

        try (InputStream is = Files.newInputStream(csvPath);
             CSVParser parser = new CSVParser(new InputStreamReader(is, StandardCharsets.UTF_8), CSVFormat.DEFAULT);
             Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Student Data");
            int rowIdx = 0;
            for (CSVRecord record : parser) {
                Row row = sheet.createRow(rowIdx++);
                for (int c = 0; c < record.size(); c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(record.get(c));
                }
            }

            try (FileOutputStream fos = new FileOutputStream(xlsxPath.toFile())) {
                workbook.write(fos);
            }
            assertThat(Files.exists(xlsxPath)).isTrue();
        }
    }
}

