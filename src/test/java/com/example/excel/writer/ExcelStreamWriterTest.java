package com.example.excel.writer;

import com.example.excel.reader.ExcelStreamReader;
import com.example.excel.writer.ExcelStreamWriter;
import com.example.model.Person;
import com.example.model.PersonWithAllTypes;
import com.example.model.PersonWithDate;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

@DisplayName("ExcelStreamWriter: Stream APIを使用したExcel書き込み")
public class ExcelStreamWriterTest {

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
        
        ExcelStreamWriter.of(Person.class, outputPath)
            .write(persons.stream());
        
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
    @DisplayName("空のStream - 空のStreamを書き込んでもヘッダーのみが作成されること")
    void testWriteEmptyStream() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("empty_stream.xlsx");
        
        ExcelStreamWriter.of(Person.class, outputPath)
            .write(Stream.empty());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet);
            
            // ヘッダー行のみ存在することを確認
            assertEquals(1, sheet.getPhysicalNumberOfRows());
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
        }
    }
    
    @Test
    @DisplayName("シート名指定 - sheetName()で作成するシート名を指定できること")
    void testWriteWithSheetName() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("with_sheet_name.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        ExcelStreamWriter.of(Person.class, outputPath)
            .sheetName("社員データ")
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheet("社員データ");
            assertNotNull(sheet);
            assertEquals("社員データ", sheet.getSheetName());
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
    
    private int getCellValueAsNumber(Cell cell) {
        if (cell == null) {
            return 0;
        }
        return (int) cell.getNumericCellValue();
    }
    
    @Test
    @DisplayName("全型の書き込み - Integer, Long, Double, Booleanを正しく書き込めること")
    void testWriteAllTypes() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("all_types_write.xlsx");
        
        List<PersonWithAllTypes> persons = List.of(
            new PersonWithAllTypes("田中太郎", 25, 1001L, 450000.50, true),
            new PersonWithAllTypes("佐藤花子", 30, 2002L, 550000.75, false),
            new PersonWithAllTypes("山田次郎", 28, 3003L, 500000.0, true)
        );
        
        ExcelStreamWriter.of(PersonWithAllTypes.class, outputPath)
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // ヘッダー行の確認
            Row headerRow = sheet.getRow(0);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
            assertEquals("年齢", getCellValueAsString(headerRow.getCell(1)));
            assertEquals("ID", getCellValueAsString(headerRow.getCell(2)));
            assertEquals("給料", getCellValueAsString(headerRow.getCell(3)));
            assertEquals("有効", getCellValueAsString(headerRow.getCell(4)));
            
            // データ行の確認
            Row row1 = sheet.getRow(1);
            assertEquals("田中太郎", getCellValueAsString(row1.getCell(0)));
            assertEquals(25, (int) row1.getCell(1).getNumericCellValue());
            assertEquals(1001L, (long) row1.getCell(2).getNumericCellValue());
            assertEquals(450000.50, row1.getCell(3).getNumericCellValue(), 0.01);
            assertTrue(row1.getCell(4).getBooleanCellValue());
            
            Row row2 = sheet.getRow(2);
            assertFalse(row2.getCell(4).getBooleanCellValue());
        }
    }
    
    @Test
    @DisplayName("日付型の書き込み - LocalDate, LocalDateTimeを正しく書き込めること")
    void testWriteDateTypes() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("date_types_write.xlsx");
        
        List<PersonWithDate> persons = List.of(
            new PersonWithDate("田中太郎", LocalDate.of(1990, 5, 15), LocalDateTime.of(2024, 1, 15, 10, 30, 0)),
            new PersonWithDate("佐藤花子", LocalDate.of(1985, 12, 25), LocalDateTime.of(2024, 2, 20, 15, 45, 30))
        );
        
        ExcelStreamWriter.of(PersonWithDate.class, outputPath)
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // ヘッダー行の確認
            Row headerRow = sheet.getRow(0);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
            assertEquals("誕生日", getCellValueAsString(headerRow.getCell(1)));
            assertEquals("登録日時", getCellValueAsString(headerRow.getCell(2)));
            
            // データ行の確認
            Row row1 = sheet.getRow(1);
            assertEquals("田中太郎", getCellValueAsString(row1.getCell(0)));
            assertNotNull(row1.getCell(1).getDateCellValue());
            assertNotNull(row1.getCell(2).getDateCellValue());
        }
    }
    
    @Test
    @DisplayName("位置ベースマッピング - usePositionMapping()で位置ベースに書き込めること")
    void testWriteWithPositionMapping() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("position_mapping_write.xlsx");
        
        List<PersonWithoutHeader> persons = List.of(
            new PersonWithoutHeader("田中太郎", 25, "エンジニア", "東京"),
            new PersonWithoutHeader("佐藤花子", 30, "デザイナー", "大阪")
        );
        
        ExcelStreamWriter.of(PersonWithoutHeader.class, outputPath)
            .usePositionMapping()
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // ヘッダー行の確認
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow.getCell(0));
            assertNotNull(headerRow.getCell(1));
            assertNotNull(headerRow.getCell(2));
            assertNotNull(headerRow.getCell(3));
            
            // データ行の確認
            Row row1 = sheet.getRow(1);
            assertEquals("田中太郎", getCellValueAsString(row1.getCell(0)));
            assertEquals(25, getCellValueAsNumber(row1.getCell(1)));
            assertEquals("エンジニア", getCellValueAsString(row1.getCell(2)));
            assertEquals("東京", getCellValueAsString(row1.getCell(3)));
        }
    }
    
    @Test
    @DisplayName("大量データの書き込み - 大量のデータでも正しく書き込めること")
    void testWriteLargeData() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("large_data_write.xlsx");
        
        List<Person> persons = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            persons.add(new Person("田中太郎" + i, 20 + (i % 50), "エンジニア", "東京"));
        }
        
        ExcelStreamWriter.of(Person.class, outputPath)
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(1001, sheet.getPhysicalNumberOfRows()); // ヘッダー + 1000行
        }
    }
    
    @Test
    @DisplayName("null値の書き込み - null値を含むデータを正しく書き込めること")
    void testWriteWithNullValues() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("null_values_write.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", null, null, "東京"),
            new Person("佐藤花子", 30, "デザイナー", null)
        );
        
        ExcelStreamWriter.of(Person.class, outputPath)
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            Row row1 = sheet.getRow(1);
            assertEquals("田中太郎", getCellValueAsString(row1.getCell(0)));
            // null値のセルは空白になる
            assertTrue(row1.getCell(1) == null || row1.getCell(1).getCellType().toString().equals("BLANK"));
        }
    }
    
    @Test
    @DisplayName("複数シートへの書き込み - 既存ワークブックへの追加はできないが、新規作成は可能なこと")
    void testWriteToNewWorkbook() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("new_workbook.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        ExcelStreamWriter.of(Person.class, outputPath)
            .sheetName("シート1")
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheet("シート1");
            assertNotNull(sheet);
            assertEquals("シート1", sheet.getSheetName());
        }
    }
    
    @Test
    @DisplayName("Reader/Writerの往復 - 書き込んだファイルをReaderで読み込めること")
    void testRoundTrip() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("round_trip.xlsx");
        
        List<Person> originalPersons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京"),
            new Person("佐藤花子", 30, "デザイナー", "大阪"),
            new Person("山田次郎", 28, "営業", "福岡")
        );
        
        // 書き込み
        ExcelStreamWriter.of(Person.class, outputPath)
            .write(originalPersons.stream());
        
        // 読み込み
        List<Person> readPersons = ExcelStreamReader.of(Person.class, outputPath)
            .process((Function<Stream<Person>, List<Person>>) stream -> stream.toList());
        
        assertEquals(3, readPersons.size());
        assertEquals("田中太郎", readPersons.get(0).getName());
        assertEquals(25, readPersons.get(0).getAge());
        assertEquals("エンジニア", readPersons.get(0).getOccupation());
        assertEquals("東京", readPersons.get(0).getBirthplace());
    }
    
    @Test
    @DisplayName("全型のReader/Writerの往復 - 全ての型が正しく往復できること")
    void testRoundTripAllTypes() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("round_trip_all_types.xlsx");
        
        List<PersonWithAllTypes> originalPersons = List.of(
            new PersonWithAllTypes("田中太郎", 25, 1001L, 450000.50, true),
            new PersonWithAllTypes("佐藤花子", 30, 2002L, 550000.75, false)
        );
        
        // 書き込み
        ExcelStreamWriter.of(PersonWithAllTypes.class, outputPath)
            .write(originalPersons.stream());
        
        // 読み込み
        List<PersonWithAllTypes> readPersons = ExcelStreamReader.of(PersonWithAllTypes.class, outputPath)
            .process((Function<Stream<PersonWithAllTypes>, List<PersonWithAllTypes>>) stream -> stream.toList());
        
        assertEquals(2, readPersons.size());
        assertEquals("田中太郎", readPersons.get(0).getName());
        assertEquals(25, readPersons.get(0).getAge());
        assertEquals(1001L, readPersons.get(0).getId());
        assertEquals(450000.50, readPersons.get(0).getSalary(), 0.01);
        assertTrue(readPersons.get(0).getActive());
    }
    
    @Test
    @DisplayName("日付型のReader/Writerの往復 - 日付型が正しく往復できること")
    void testRoundTripDateTypes() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("round_trip_date_types.xlsx");
        
        List<PersonWithDate> originalPersons = List.of(
            new PersonWithDate("田中太郎", LocalDate.of(1990, 5, 15), LocalDateTime.of(2024, 1, 15, 10, 30, 0))
        );
        
        // 書き込み
        ExcelStreamWriter.of(PersonWithDate.class, outputPath)
            .write(originalPersons.stream());
        
        // 読み込み
        List<PersonWithDate> readPersons = ExcelStreamReader.of(PersonWithDate.class, outputPath)
            .process((Function<Stream<PersonWithDate>, List<PersonWithDate>>) stream -> stream.toList());
        
        assertEquals(1, readPersons.size());
        assertEquals("田中太郎", readPersons.get(0).getName());
        assertEquals(LocalDate.of(1990, 5, 15), readPersons.get(0).getBirthDate());
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 30, 0), readPersons.get(0).getRegisteredAt());
    }
    
    @Test
    @DisplayName("ファイル書き込みエラー - 無効なパスへの書き込みはIOExceptionを投げること")
    void testWriteToInvalidPath() {
        Path invalidPath = Paths.get("Z:\\invalid\\path\\test.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        assertThrows(IOException.class, () -> {
            ExcelStreamWriter.of(Person.class, invalidPath)
                .write(persons.stream());
        });
    }
    
    @Test
    @DisplayName("シートインデックス指定 - sheetIndex()でシート番号を指定できること（現在は新規作成のみのため動作確認）")
    void testWriteWithSheetIndex() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("with_sheet_index.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        // sheetIndex() メソッドを呼び出す（カバレッジ向上のため）
        ExcelStreamWriter.of(Person.class, outputPath)
            .sheetIndex(0)
            .sheetName("データ")
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheet("データ");
            assertNotNull(sheet);
        }
    }
    
    @Test
    @DisplayName("ヘッダーマッピング明示 - useHeaderMapping()を明示的に呼び出せること")
    void testWriteWithExplicitHeaderMapping() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("explicit_header_mapping.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京"),
            new Person("佐藤花子", 30, "デザイナー", "大阪")
        );
        
        ExcelStreamWriter.of(Person.class, outputPath)
            .useHeaderMapping()
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
            assertEquals("年齢", getCellValueAsString(headerRow.getCell(1)));
        }
    }
    
    @Test
    @DisplayName("メソッドチェーン - 複数のビルダーメソッドをチェーンできること")
    void testMethodChaining() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("method_chaining.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        ExcelStreamWriter.of(Person.class, outputPath)
            .sheetName("テスト")
            .sheetIndex(0)
            .useHeaderMapping()
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
    }
    
    @Test
    @DisplayName("複雑なセル値 - 特殊な値を含むデータを書き込めること")
    void testWriteComplexValues() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("complex_values.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中\"太郎\"", 25, "エンジニア\n改行あり", "東京"),
            new Person("佐藤,花子", 30, "デザイナー", "大阪,京都")
        );
        
        ExcelStreamWriter.of(Person.class, outputPath)
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        // 読み込んで確認
        List<Person> readPersons = ExcelStreamReader.of(Person.class, outputPath)
            .process((Function<Stream<Person>, List<Person>>) stream -> stream.toList());
        
        assertEquals(2, readPersons.size());
        assertEquals("田中\"太郎\"", readPersons.get(0).getName());
        assertEquals("佐藤,花子", readPersons.get(1).getName());
    }
    
    @Test
    @DisplayName("書き込み位置指定 - startCell()で開始セルを指定できること")
    void testWriteWithStartCell() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("start_cell.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京"),
            new Person("佐藤花子", 30, "デザイナー", "大阪")
        );
        
        // B3セル（行2、列1）から書き込み開始
        ExcelStreamWriter.of(Person.class, outputPath)
            .startCell(2, 1)
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // B3セル（行2、列1）にヘッダーがあることを確認
            Row headerRow = sheet.getRow(2);
            assertNotNull(headerRow);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(1)));
            assertEquals("年齢", getCellValueAsString(headerRow.getCell(2)));
            
            // B4セル（行3、列1）にデータがあることを確認
            Row dataRow1 = sheet.getRow(3);
            assertNotNull(dataRow1);
            assertEquals("田中太郎", getCellValueAsString(dataRow1.getCell(1)));
            assertEquals(25, getCellValueAsNumber(dataRow1.getCell(2)));
        }
    }
    
    @Test
    @DisplayName("既存ファイルへの書き込み - テンプレートファイルにデータを書き込めること")
    void testWriteToExistingFile() throws IOException {
        // テンプレートファイルを作成
        Path templatePath = TEST_OUTPUT_DIR.resolve("template.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(templatePath.toFile())) {
            
            Sheet sheet = workbook.createSheet("テンプレート");
            
            // タイトル行（A1セル）
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("社員マスタ");
            
            // 説明行（A2セル）
            Row descRow = sheet.createRow(1);
            descRow.createCell(0).setCellValue("2024年度版");
            
            workbook.write(fos);
        }
        
        // テンプレートファイルにデータを追記
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京"),
            new Person("佐藤花子", 30, "デザイナー", "大阪")
        );
        
        ExcelStreamWriter.of(Person.class, templatePath)
            .sheetName("テンプレート")
            .startCell(2, 0)  // A3セルから書き込み
            .loadExisting()
            .write(persons.stream());
        
        // 結果確認
        try (FileInputStream fis = new FileInputStream(templatePath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheet("テンプレート");
            assertNotNull(sheet);
            
            // タイトルが残っていることを確認
            Row titleRow = sheet.getRow(0);
            assertEquals("社員マスタ", getCellValueAsString(titleRow.getCell(0)));
            
            // 説明が残っていることを確認
            Row descRow = sheet.getRow(1);
            assertEquals("2024年度版", getCellValueAsString(descRow.getCell(0)));
            
            // A3セルにヘッダーがあることを確認
            Row headerRow = sheet.getRow(2);
            assertNotNull(headerRow);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
            
            // A4セルにデータがあることを確認
            Row dataRow1 = sheet.getRow(3);
            assertNotNull(dataRow1);
            assertEquals("田中太郎", getCellValueAsString(dataRow1.getCell(0)));
            assertEquals(25, getCellValueAsNumber(dataRow1.getCell(1)));
        }
    }
    
    @Test
    @DisplayName("既存ファイル・新規シート - 既存ファイルに新しいシートを追加できること")
    void testWriteToExistingFileNewSheet() throws IOException {
        // 既存ファイルを作成
        Path existingPath = TEST_OUTPUT_DIR.resolve("existing_with_sheets.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(existingPath.toFile())) {
            
            Sheet sheet1 = workbook.createSheet("既存シート");
            Row row = sheet1.createRow(0);
            row.createCell(0).setCellValue("既存データ");
            
            workbook.write(fos);
        }
        
        // 新しいシートを追加
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        ExcelStreamWriter.of(Person.class, existingPath)
            .sheetName("新規シート")
            .loadExisting()
            .write(persons.stream());
        
        // 結果確認
        try (FileInputStream fis = new FileInputStream(existingPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            // 既存シートが残っていることを確認
            Sheet existingSheet = workbook.getSheet("既存シート");
            assertNotNull(existingSheet);
            assertEquals("既存データ", getCellValueAsString(existingSheet.getRow(0).getCell(0)));
            
            // 新しいシートが追加されていることを確認
            Sheet newSheet = workbook.getSheet("新規シート");
            assertNotNull(newSheet);
            Row headerRow = newSheet.getRow(0);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
        }
    }
    
    @Test
    @DisplayName("startCell単独使用 - loadExisting()なしでもstartCell()は動作すること")
    void testStartCellWithoutLoadExisting() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("start_cell_only.xlsx");
        
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京")
        );
        
        // 新規ファイルでもstartCell()を使用可能
        ExcelStreamWriter.of(Person.class, outputPath)
            .startCell(5, 3)  // D6セルから開始
            .write(persons.stream());
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // D6セル（行5、列3）にヘッダーがあることを確認
            Row headerRow = sheet.getRow(5);
            assertNotNull(headerRow);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(3)));
        }
    }
}

