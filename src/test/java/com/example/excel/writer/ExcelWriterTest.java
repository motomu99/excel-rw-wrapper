package com.example.excel.writer;

import com.example.excel.reader.ExcelReader;
import com.example.model.Person;
import com.example.model.PersonWithoutHeader;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@DisplayName("ExcelWriter: 一括書き込み")
public class ExcelWriterTest {

    private static final Path TEST_OUTPUT_DIR = Paths.get("build/test-outputs");
    
    @BeforeAll
    static void setUp() throws IOException {
        // テスト出力ディレクトリを作成
        Files.createDirectories(TEST_OUTPUT_DIR);
    }

    @Test
    @DisplayName("基本的な書き込み - Excelファイルにデータを書き込めること")
    void testBasicWrite() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("basic_write.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京"),
            new Person("佐藤花子", 30, "デザイナー", "大阪"),
            new Person("山田次郎", 28, "営業", "福岡")
        );
        
        ExcelWriter.builder(Person.class, outputPath)
            .write(persons);
        
        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));
        
        // ファイルの内容を確認
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet);
            
            // ヘッダー行の確認
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
            assertEquals("年齢", getCellValueAsString(headerRow.getCell(1)));
            assertEquals("職業", getCellValueAsString(headerRow.getCell(2)));
            assertEquals("出身地", getCellValueAsString(headerRow.getCell(3)));
            
            // データ行の確認
            assertEquals(4, sheet.getPhysicalNumberOfRows()); // ヘッダー + 3行
            
            Row row1 = sheet.getRow(1);
            assertEquals("田中太郎", getCellValueAsString(row1.getCell(0)));
            assertEquals(25, getCellValueAsNumber(row1.getCell(1)));
            assertEquals("エンジニア", getCellValueAsString(row1.getCell(2)));
            assertEquals("東京", getCellValueAsString(row1.getCell(3)));
        }
    }
    
    @Test
    @DisplayName("空のList - 空のListを書き込んでもヘッダーのみが作成されること")
    void testWriteEmptyList() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("empty_list.xlsx");
        
        ExcelWriter.builder(Person.class, outputPath)
            .write(List.of());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet);
            
            // ヘッダー行のみ存在
            assertEquals(1, sheet.getPhysicalNumberOfRows());
            
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
        }
    }

    @Test
    @DisplayName("シート名指定 - シート名を指定して書き込めること")
    void testWriteWithSheetName() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("sheet_name.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        ExcelWriter.builder(Person.class, outputPath)
            .sheetName("社員データ")
            .write(persons);
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheet("社員データ");
            assertNotNull(sheet);
            assertEquals("社員データ", sheet.getSheetName());
        }
    }

    @Test
    @DisplayName("位置ベースマッピング - usePositionMapping()でヘッダーなしで書き込めること")
    void testWriteWithPositionMapping() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("position_mapping.xlsx");
        
        List<PersonWithoutHeader> persons = List.of(
            new PersonWithoutHeader("田中太郎", 25, "エンジニア", "東京"),
            new PersonWithoutHeader("佐藤花子", 30, "デザイナー", "大阪")
        );
        
        ExcelWriter.builder(PersonWithoutHeader.class, outputPath)
            .usePositionMapping()
            .write(persons);
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet);
            
            // ヘッダー行の確認（フィールド名が使われる）
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);
            assertEquals("name", getCellValueAsString(headerRow.getCell(0)));
            assertEquals("age", getCellValueAsString(headerRow.getCell(1)));
            
            // データ行の確認
            Row row1 = sheet.getRow(1);
            assertEquals("田中太郎", getCellValueAsString(row1.getCell(0)));
            assertEquals(25, getCellValueAsNumber(row1.getCell(1)));
        }
    }

    @Test
    @DisplayName("ヘッダーマッピング - useHeaderMapping()でヘッダー付きで書き込めること")
    void testWriteWithHeaderMapping() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("header_mapping.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        ExcelWriter.builder(Person.class, outputPath)
            .useHeaderMapping()
            .write(persons);
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet);
            
            // ヘッダー行の確認
            Row headerRow = sheet.getRow(0);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
            assertEquals("年齢", getCellValueAsString(headerRow.getCell(1)));
        }
    }

    @Test
    @DisplayName("ヘッダーなし書き込み - noHeader()でヘッダーなしで書き込めること")
    void testWriteNoHeader() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("no_header_writer.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        ExcelWriter.builder(Person.class, outputPath)
            .noHeader()
            .write(persons);
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // 行数がデータ数（1）と一致することを確認
            assertEquals(1, sheet.getPhysicalNumberOfRows());
            
            // 1行目がデータであることを確認
            Row row = sheet.getRow(0);
            assertEquals("田中太郎", getCellValueAsString(row.getCell(0)));
        }
    }

    @Test
    @DisplayName("既存ファイルへの書き込み - loadExisting()で既存ファイルに追記できること")
    void testWriteToExistingFile() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("existing_file.xlsx");
        
        // 最初にテンプレートファイルを作成
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(outputPath.toFile())) {
            
            Sheet sheet = workbook.createSheet("データ");
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("社員一覧");
            
            workbook.write(fos);
        }
        
        // 既存ファイルにデータを書き込む
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        ExcelWriter.builder(Person.class, outputPath)
            .loadExisting()
            .sheetName("データ")
            .startCell(2, 0)  // A3セルから書き込み
            .write(persons);
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheet("データ");
            assertNotNull(sheet);
            
            // タイトル行が残っている
            Row titleRow = sheet.getRow(0);
            assertEquals("社員一覧", getCellValueAsString(titleRow.getCell(0)));
            
            // ヘッダー行がA3に書き込まれている
            Row headerRow = sheet.getRow(2);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
            
            // データ行がA4に書き込まれている
            Row dataRow = sheet.getRow(3);
            assertEquals("田中太郎", getCellValueAsString(dataRow.getCell(0)));
        }
    }

    @Test
    @DisplayName("開始セル指定 - startCell()で指定した位置から書き込めること")
    void testWriteWithStartCell() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("start_cell.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        ExcelWriter.builder(Person.class, outputPath)
            .startCell(2, 1)  // B3セルから書き込み
            .write(persons);
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // ヘッダー行がB3に書き込まれている
            Row headerRow = sheet.getRow(2);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(1)));
            
            // データ行がB4に書き込まれている
            Row dataRow = sheet.getRow(3);
            assertEquals("田中太郎", getCellValueAsString(dataRow.getCell(1)));
        }
    }

    @Test
    @DisplayName("メソッドチェーン - 複数の設定を組み合わせて使用できること")
    void testWriteWithChainedSettings() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("chained_settings.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京"),
            new Person("佐藤花子", 30, "デザイナー", "大阪")
        );
        
        ExcelWriter.builder(Person.class, outputPath)
            .sheetName("社員データ")
            .useHeaderMapping()
            .startCell(1, 0)
            .write(persons);
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheet("社員データ");
            assertNotNull(sheet);
            
            // ヘッダー行がA2に書き込まれている
            Row headerRow = sheet.getRow(1);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
            
            // データ行がA3, A4に書き込まれている
            Row dataRow1 = sheet.getRow(2);
            assertEquals("田中太郎", getCellValueAsString(dataRow1.getCell(0)));
            
            Row dataRow2 = sheet.getRow(3);
            assertEquals("佐藤花子", getCellValueAsString(dataRow2.getCell(0)));
        }
    }

    @Test
    @DisplayName("読み込みと書き込みの連携 - ExcelReaderで読み込んだデータをExcelWriterで書き込めること")
    void testReadAndWrite() throws IOException {
        Path inputPath = TEST_OUTPUT_DIR.resolve("read_write_input.xlsx");
        Path outputPath = TEST_OUTPUT_DIR.resolve("read_write_output.xlsx");
        
        // まずテスト用のExcelファイルを作成
        List<Person> originalPersons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京"),
            new Person("佐藤花子", 30, "デザイナー", "大阪")
        );
        
        ExcelWriter.builder(Person.class, inputPath)
            .write(originalPersons);
        
        // ExcelReaderで読み込む
        List<Person> readPersons = ExcelReader.builder(Person.class, inputPath)
            .read();
        
        assertNotNull(readPersons);
        assertEquals(2, readPersons.size());
        
        // ExcelWriterで書き込む
        ExcelWriter.builder(Person.class, outputPath)
            .write(readPersons);
        
        // 書き込んだファイルを確認
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(3, sheet.getPhysicalNumberOfRows()); // ヘッダー + 2行
            
            Row dataRow1 = sheet.getRow(1);
            assertEquals("田中太郎", getCellValueAsString(dataRow1.getCell(0)));
            
            Row dataRow2 = sheet.getRow(2);
            assertEquals("佐藤花子", getCellValueAsString(dataRow2.getCell(0)));
        }
    }

    @Test
    @DisplayName("大量データ - 大量のデータを書き込めること")
    void testWriteLargeData() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("large_data.xlsx");
        
        // 1000件のデータを作成
        List<Person> persons = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            persons.add(new Person("名前" + i, 20 + i % 50, "職業" + i, "出身地" + i));
        }
        
        ExcelWriter.builder(Person.class, outputPath)
            .write(persons);
        
        assertTrue(Files.exists(outputPath));
        
        // ファイルの内容を確認
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(1001, sheet.getPhysicalNumberOfRows()); // ヘッダー + 1000行
            
            // 最初のデータ
            Row row1 = sheet.getRow(1);
            assertEquals("名前0", getCellValueAsString(row1.getCell(0)));
            
            // 最後のデータ
            Row row1000 = sheet.getRow(1000);
            assertEquals("名前999", getCellValueAsString(row1000.getCell(0)));
        }
    }

    // ヘルパーメソッド
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private int getCellValueAsNumber(Cell cell) {
        if (cell == null) return 0;
        return (int) cell.getNumericCellValue();
    }
}

