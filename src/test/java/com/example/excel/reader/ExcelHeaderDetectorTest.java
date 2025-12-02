package com.example.excel.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.exception.HeaderNotFoundException;
import com.example.exception.KeyColumnNotFoundException;

@DisplayName("ExcelHeaderDetector テスト")
class ExcelHeaderDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("ヘッダー検出 - 空白を含むヘッダー名を正規化して検出できること")
    void testDetectHeader_WithWhitespace() throws IOException, HeaderNotFoundException, KeyColumnNotFoundException {
        Path excelFile = tempDir.resolve("whitespace_header.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFile.toFile())) {
            
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("  名前  "); // 前後空白あり
            headerRow.createCell(1).setCellValue("年齢");
            
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("田中太郎");
            dataRow.createCell(1).setCellValue(25);
            
            workbook.write(fos);
        }

        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(excelFile))) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // キー列 "名前" で検出（Excel側は "  名前  "）
            ExcelHeaderDetector detector = new ExcelHeaderDetector("名前", 10);
            boolean found = detector.detectHeader(rowIterator);

            assertTrue(found, "ヘッダーが見つかること");
            
            Map<String, Integer> columnMap = detector.getColumnMap();
            assertTrue(columnMap.containsKey("名前"), "正規化されたカラム名 '名前' がマップに含まれること");
            assertEquals(0, columnMap.get("名前"));
            
            Map<Integer, String> headerMap = detector.getHeaderMap();
            assertEquals("名前", headerMap.get(0), "インデックス0のヘッダー名が正規化されていること");
        }
    }
    
    @Test
    @DisplayName("ヘッダー検出 - キー列指定自体に空白があってもマッチすること")
    void testDetectHeader_WithWhitespaceInKey() throws IOException, HeaderNotFoundException, KeyColumnNotFoundException {
        Path excelFile = tempDir.resolve("normal_header.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFile.toFile())) {
            
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前"); 
            
            workbook.write(fos);
        }

        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(excelFile))) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // キー列 "  名前  " で検出（Excel側は "名前"）
            ExcelHeaderDetector detector = new ExcelHeaderDetector("  名前  ", 10);
            boolean found = detector.detectHeader(rowIterator);

            assertTrue(found, "ヘッダーが見つかること");
        }
    }

    @Test
    @DisplayName("ヘッダー検出 - 複数行目にあるヘッダーを検出できること")
    void testDetectHeader_SearchRows() throws IOException, HeaderNotFoundException, KeyColumnNotFoundException {
        Path excelFile = tempDir.resolve("header_at_row3.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFile.toFile())) {
            
            Sheet sheet = workbook.createSheet("Sheet1");
            sheet.createRow(0).createCell(0).setCellValue("Title");
            sheet.createRow(1); // Empty
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Name");
            
            workbook.write(fos);
        }

        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(excelFile))) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            ExcelHeaderDetector detector = new ExcelHeaderDetector("ID", 5);
            boolean found = detector.detectHeader(rowIterator);

            assertTrue(found);
            assertEquals(2, detector.getHeaderRow().getRowNum());
        }
    }
}

