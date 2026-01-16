package com.example.excel.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.exception.HeaderNotFoundException;
import com.example.exception.KeyColumnNotFoundException;

@DisplayName("FastExcelHeaderDetector テスト")
class FastExcelHeaderDetectorTest {

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

        try (InputStream inputStream = Files.newInputStream(excelFile);
             ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            org.dhatim.fastexcel.reader.Sheet sheet = workbook.getFirstSheet();
            Stream<org.dhatim.fastexcel.reader.Row> rowStream = sheet.openStream();
            Iterator<org.dhatim.fastexcel.reader.Row> rowIterator = rowStream.iterator();

            // キー列 "名前" で検出（Excel側は "  名前  "）
            FastExcelHeaderDetector detector = new FastExcelHeaderDetector("名前", 10);
            boolean found = detector.detectHeader(rowIterator);

            assertTrue(found, "ヘッダーが見つかること");
            
            Map<String, Integer> columnMap = detector.getColumnMap();
            assertTrue(columnMap.containsKey("名前"), "正規化されたカラム名 '名前' がマップに含まれること");
            assertEquals(0, columnMap.get("名前"));
            
            Map<Integer, String> headerMap = detector.getHeaderMap();
            assertEquals("名前", headerMap.get(0), "インデックス0のヘッダー名が正規化されていること");
            
            rowStream.close();
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

        try (InputStream inputStream = Files.newInputStream(excelFile);
             ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            org.dhatim.fastexcel.reader.Sheet sheet = workbook.getFirstSheet();
            Stream<org.dhatim.fastexcel.reader.Row> rowStream = sheet.openStream();
            Iterator<org.dhatim.fastexcel.reader.Row> rowIterator = rowStream.iterator();

            // キー列 "  名前  " で検出（Excel側は "名前"）
            FastExcelHeaderDetector detector = new FastExcelHeaderDetector("  名前  ", 10);
            boolean found = detector.detectHeader(rowIterator);

            assertTrue(found, "ヘッダーが見つかること");
            
            rowStream.close();
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

        try (InputStream inputStream = Files.newInputStream(excelFile);
             ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            org.dhatim.fastexcel.reader.Sheet sheet = workbook.getFirstSheet();
            Stream<org.dhatim.fastexcel.reader.Row> rowStream = sheet.openStream();
            Iterator<org.dhatim.fastexcel.reader.Row> rowIterator = rowStream.iterator();

            FastExcelHeaderDetector detector = new FastExcelHeaderDetector("ID", 5);
            boolean found = detector.detectHeader(rowIterator);

            assertTrue(found);
            // fastexcelのRow.getRowNum()は1始まりなので、0始まりに変換すると2になる
            assertEquals(2, detector.getHeaderRowIndex());
            
            rowStream.close();
        }
    }

    @Test
    @DisplayName("ヘッダー検出 - A列が空でB列から始まるヘッダーを検出できること")
    void testDetectHeader_StartFromColumnB() throws IOException, HeaderNotFoundException, KeyColumnNotFoundException {
        Path excelFile = tempDir.resolve("header_start_b.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFile.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(1).setCellValue("ID");
            headerRow.createCell(2).setCellValue("Name");

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(1).setCellValue(1);
            dataRow.createCell(2).setCellValue("Alice");

            workbook.write(fos);
        }

        try (InputStream inputStream = Files.newInputStream(excelFile);
             ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            org.dhatim.fastexcel.reader.Sheet sheet = workbook.getFirstSheet();
            Stream<org.dhatim.fastexcel.reader.Row> rowStream = sheet.openStream();
            Iterator<org.dhatim.fastexcel.reader.Row> rowIterator = rowStream.iterator();

            FastExcelHeaderDetector detector = new FastExcelHeaderDetector("ID", 5);
            boolean found = detector.detectHeader(rowIterator);

            assertTrue(found, "ヘッダーが見つかること");
            Map<String, Integer> columnMap = detector.getColumnMap();
            assertEquals(1, columnMap.get("ID"));
            assertEquals(2, columnMap.get("Name"));

            Map<Integer, String> headerMap = detector.getHeaderMap();
            assertEquals("ID", headerMap.get(1));
            assertEquals("Name", headerMap.get(2));

            rowStream.close();
        }
    }

    @Test
    @DisplayName("ヘッダー検出 - スパースなヘッダー行でも検出できること")
    void testDetectHeader_SparseHeaderRow() throws IOException, HeaderNotFoundException, KeyColumnNotFoundException {
        Path excelFile = tempDir.resolve("sparse_header.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFile.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(1).setCellValue("ID");
            headerRow.createCell(3).setCellValue("Name");

            Row dataRow = sheet.createRow(1);
            dataRow.createCell(1).setCellValue(1);
            dataRow.createCell(3).setCellValue("Alice");

            workbook.write(fos);
        }

        try (InputStream inputStream = Files.newInputStream(excelFile);
             ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            org.dhatim.fastexcel.reader.Sheet sheet = workbook.getFirstSheet();
            Stream<org.dhatim.fastexcel.reader.Row> rowStream = sheet.openStream();
            Iterator<org.dhatim.fastexcel.reader.Row> rowIterator = rowStream.iterator();

            FastExcelHeaderDetector detector = new FastExcelHeaderDetector("Name", 5);
            boolean found = detector.detectHeader(rowIterator);

            assertTrue(found, "ヘッダーが見つかること");
            Map<String, Integer> columnMap = detector.getColumnMap();
            assertEquals(1, columnMap.get("ID"));
            assertEquals(3, columnMap.get("Name"));

            Map<Integer, String> headerMap = detector.getHeaderMap();
            assertEquals("ID", headerMap.get(1));
            assertEquals("Name", headerMap.get(3));

            rowStream.close();
        }
    }
}
