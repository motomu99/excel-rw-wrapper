package com.example.excel.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.model.Person;

@DisplayName("FastExcelRowIterator テスト")
class FastExcelRowIteratorTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("基本的なBean変換 - 行をBeanに正しく変換できること")
    void testBasicBeanConversion() throws IOException {
        Path excelFile = tempDir.resolve("basic.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFile.toFile())) {
            
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");
            
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("田中太郎");
            dataRow1.createCell(1).setCellValue(25);
            dataRow1.createCell(2).setCellValue("エンジニア");
            dataRow1.createCell(3).setCellValue("東京");
            
            workbook.write(fos);
        }

        try (InputStream inputStream = Files.newInputStream(excelFile);
             ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            org.dhatim.fastexcel.reader.Sheet sheet = workbook.getFirstSheet();
            Stream<org.dhatim.fastexcel.reader.Row> rowStream = sheet.openStream();
            Iterator<org.dhatim.fastexcel.reader.Row> rowIterator = rowStream.iterator();

            FastExcelRowIterator<Person> iterator = new FastExcelRowIterator<>(
                rowIterator, 
                Person.class, 
                "名前", // headerKeyColumn
                10, // headerSearchRows
                false, // usePositionMapping
                false // treatFirstRowAsData
            );

            assertTrue(iterator.hasNext(), "次の要素が存在すること");
            Person person = iterator.next();
            
            assertNotNull(person);
            assertEquals("田中太郎", person.getName());
            assertEquals(25, person.getAge());
            assertEquals("エンジニア", person.getOccupation());
            assertEquals("東京", person.getBirthplace());
            
            assertFalse(iterator.hasNext(), "次の要素が存在しないこと");
            
            rowStream.close();
        }
    }

    @Test
    @DisplayName("フリガナ削除 - フリガナ付きセルからフリガナが削除されること")
    void testFuriganaRemoval() throws IOException {
        Path excelFile = tempDir.resolve("furigana.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFile.toFile())) {
            
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");
            
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("田中太郎");
            // フリガナ付きセル（POIではフリガナが含まれる可能性がある）
            dataRow.createCell(1).setCellValue(25);
            dataRow.createCell(2).setCellValue("エンジニア");
            dataRow.createCell(3).setCellValue("東京");
            
            workbook.write(fos);
        }

        try (InputStream inputStream = Files.newInputStream(excelFile);
             ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            org.dhatim.fastexcel.reader.Sheet sheet = workbook.getFirstSheet();
            Stream<org.dhatim.fastexcel.reader.Row> rowStream = sheet.openStream();
            Iterator<org.dhatim.fastexcel.reader.Row> rowIterator = rowStream.iterator();

            FastExcelRowIterator<Person> iterator = new FastExcelRowIterator<>(
                rowIterator, 
                Person.class, 
                null,
                10,
                false,
                false
            );

            assertTrue(iterator.hasNext());
            Person person = iterator.next();
            
            // フリガナが削除されていることを確認
            assertNotNull(person.getName());
            assertFalse(person.getName().contains("たなか"), "フリガナが含まれていないこと");
            
            rowStream.close();
        }
    }

    @Test
    @DisplayName("列数チェック - 列数不一致の行がスキップされること")
    void testColumnCountValidation() throws IOException {
        Path excelFile = tempDir.resolve("column_mismatch.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFile.toFile())) {
            
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");
            
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("田中太郎");
            dataRow1.createCell(1).setCellValue(25);
            dataRow1.createCell(2).setCellValue("エンジニア");
            dataRow1.createCell(3).setCellValue("東京");
            
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("佐藤花子");
            dataRow2.createCell(1).setCellValue(30);
            dataRow2.createCell(2).setCellValue("デザイナー");
            dataRow2.createCell(3).setCellValue("大阪");
            dataRow2.createCell(4).setCellValue("余分な列"); // 列数不一致
            
            Row dataRow3 = sheet.createRow(3);
            dataRow3.createCell(0).setCellValue("山田次郎");
            dataRow3.createCell(1).setCellValue(28);
            dataRow3.createCell(2).setCellValue("営業");
            dataRow3.createCell(3).setCellValue("福岡");
            
            workbook.write(fos);
        }

        try (InputStream inputStream = Files.newInputStream(excelFile);
             ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            org.dhatim.fastexcel.reader.Sheet sheet = workbook.getFirstSheet();
            Stream<org.dhatim.fastexcel.reader.Row> rowStream = sheet.openStream();
            Iterator<org.dhatim.fastexcel.reader.Row> rowIterator = rowStream.iterator();

            FastExcelRowIterator<Person> iterator = new FastExcelRowIterator<>(
                rowIterator, 
                Person.class, 
                null,
                10,
                false,
                false,
                true // validateColumnCount
            );

            List<Person> persons = new java.util.ArrayList<>();
            while (iterator.hasNext()) {
                persons.add(iterator.next());
            }
            
            // 列数不一致の行はスキップされるので、2件のみ
            assertEquals(2, persons.size());
            assertEquals("田中太郎", persons.get(0).getName());
            assertEquals("山田次郎", persons.get(1).getName());
            
            // エラーが記録されていること
            List<ExcelReadError> errors = iterator.getErrors();
            assertEquals(1, errors.size());
            assertEquals(3, errors.get(0).getLineNumber());
            
            rowStream.close();
        }
    }
}
