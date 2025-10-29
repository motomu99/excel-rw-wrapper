package com.example.csv;

import com.example.csv.model.Person;
import com.example.csv.model.PersonWithAllTypes;
import com.example.csv.model.PersonWithDate;
import com.example.csv.model.PersonWithoutHeader;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ExcelWriterWrapperの新しいBuilderパターンAPIのテスト
 * 
 * <p>このクラスはCsvWriterWrapperと同じテイストでExcelファイルを書き込むテストです。</p>
 */
@DisplayName("ExcelWriterWrapper: 新しいBuilderパターンAPI")
public class ExcelWriterWrapperTest {

    private List<Path> filesToDelete = new ArrayList<>();

    @AfterEach
    void cleanup() throws IOException {
        for (Path path : filesToDelete) {
            if (Files.exists(path)) {
                Files.delete(path);
            }
        }
        filesToDelete.clear();
    }

    @Test
    @DisplayName("基本的な書き込み - デフォルト設定でExcelファイルに書き込めること")
    void testBuilderBasicWrite() throws IOException {
        // Builderパターンの基本的な書き込み
        Path outputPath = Paths.get("src/test/resources/excel_builder_output_test.xlsx");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("鈴木花子", 32, "デザイナー", "京都"));

        ExcelWriterWrapper.builder(Person.class, outputPath)
            .write(persons);

        assertTrue(Files.exists(outputPath));

        // 読み込んで検証
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet);
            
            // ヘッダー行の確認
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
            assertEquals("年齢", getCellValueAsString(headerRow.getCell(1)));
            
            // データ行の確認
            Row row1 = sheet.getRow(1);
            assertEquals("山田太郎", getCellValueAsString(row1.getCell(0)));
            assertEquals(28, getCellValueAsNumber(row1.getCell(1)));
        }
    }

    @Test
    @DisplayName("シート名指定 - sheetName()でシート名を指定できること")
    void testBuilderWithSheetName() throws IOException {
        Path outputPath = Paths.get("src/test/resources/excel_builder_sheet_test.xlsx");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("テスト太郎", 25, "エンジニア", "東京"));

        ExcelWriterWrapper.builder(Person.class, outputPath)
            .sheetName("社員データ")
            .write(persons);

        assertTrue(Files.exists(outputPath));

        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheet("社員データ");
            assertNotNull(sheet);
            assertEquals("社員データ", sheet.getSheetName());
        }
    }

    @Test
    @DisplayName("位置ベースマッピング - ヘッダーなしで書き込み、読み込みできること")
    void testBuilderWithPositionMapping() throws IOException {
        Path outputPath = Paths.get("src/test/resources/excel_builder_position_test.xlsx");
        filesToDelete.add(outputPath);

        List<PersonWithoutHeader> persons = new ArrayList<>();
        persons.add(new PersonWithoutHeader("テスト太郎", 25, "エンジニア", "東京"));

        ExcelWriterWrapper.builder(PersonWithoutHeader.class, outputPath)
            .usePositionMapping()
            .write(persons);

        assertTrue(Files.exists(outputPath));

        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);
            
            Row row1 = sheet.getRow(1);
            assertEquals("テスト太郎", getCellValueAsString(row1.getCell(0)));
            assertEquals(25, getCellValueAsNumber(row1.getCell(1)));
        }
    }

    @Test
    @DisplayName("空リスト - 空のリストを書き込んでもエラーが発生しないこと")
    void testBuilderWithEmptyList() throws IOException {
        Path outputPath = Paths.get("src/test/resources/excel_builder_empty_test.xlsx");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();

        ExcelWriterWrapper.builder(Person.class, outputPath)
            .write(persons);

        assertTrue(Files.exists(outputPath));
    }

    @Test
    @DisplayName("ラウンドトリップ - 書き込んだデータを読み込み、元のデータと一致すること")
    void testBuilderRoundtrip() throws IOException {
        Path outputPath = Paths.get("src/test/resources/excel_builder_roundtrip_test.xlsx");
        filesToDelete.add(outputPath);

        List<Person> originalPersons = new ArrayList<>();
        originalPersons.add(new Person("ラウンドトリップ太郎", 22, "学生", "千葉"));
        originalPersons.add(new Person("ラウンドトリップ花子", 24, "大学院生", "埼玉"));

        // 書き込み
        ExcelWriterWrapper.builder(Person.class, outputPath)
            .write(originalPersons);

        // 読み込み
        List<Person> readPersons = ExcelStreamReader.of(Person.class, outputPath)
            .process(stream -> stream.toList());

        // データが一致することを確認
        assertEquals(originalPersons.size(), readPersons.size());
        for (int i = 0; i < originalPersons.size(); i++) {
            assertEquals(originalPersons.get(i).getName(), readPersons.get(i).getName());
            assertEquals(originalPersons.get(i).getAge(), readPersons.get(i).getAge());
        }
    }

    @Test
    @DisplayName("複数設定の組み合わせ - sheetName/useHeaderMappingを同時指定できること")
    void testBuilderWithMultipleSettings() throws IOException {
        Path outputPath = Paths.get("src/test/resources/excel_builder_multi_test.xlsx");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("テスト太郎", 25, "エンジニア", "東京"));
        persons.add(new Person("テスト花子", 30, "デザイナー", "大阪"));

        ExcelWriterWrapper.builder(Person.class, outputPath)
            .sheetName("テストシート")
            .useHeaderMapping()
            .write(persons);

        assertTrue(Files.exists(outputPath));

        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheet("テストシート");
            assertNotNull(sheet);
            assertEquals(3, sheet.getPhysicalNumberOfRows()); // ヘッダー + 2行
        }
    }

    @Test
    @DisplayName("日付型の書き込み - LocalDate, LocalDateTimeを正しく書き込めること")
    void testBuilderWithDateTypes() throws IOException {
        Path outputPath = Paths.get("src/test/resources/excel_builder_date_test.xlsx");
        filesToDelete.add(outputPath);

        List<PersonWithDate> persons = new ArrayList<>();
        persons.add(new PersonWithDate("田中太郎", LocalDate.of(1990, 5, 15), LocalDateTime.of(2024, 1, 15, 10, 30, 0)));

        ExcelWriterWrapper.builder(PersonWithDate.class, outputPath)
            .write(persons);

        assertTrue(Files.exists(outputPath));

        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Row row1 = sheet.getRow(1);
            assertNotNull(row1.getCell(1).getDateCellValue());
            assertNotNull(row1.getCell(2).getDateCellValue());
        }
    }

    @Test
    @DisplayName("全型の書き込み - Integer, Long, Double, Booleanを正しく書き込めること")
    void testBuilderWithAllTypes() throws IOException {
        Path outputPath = Paths.get("src/test/resources/excel_builder_alltypes_test.xlsx");
        filesToDelete.add(outputPath);

        List<PersonWithAllTypes> persons = new ArrayList<>();
        persons.add(new PersonWithAllTypes("田中太郎", 25, 1001L, 450000.50, true));
        persons.add(new PersonWithAllTypes("佐藤花子", 30, 2002L, 550000.75, false));

        ExcelWriterWrapper.builder(PersonWithAllTypes.class, outputPath)
            .write(persons);

        assertTrue(Files.exists(outputPath));

        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Row row1 = sheet.getRow(1);
            assertEquals(25, (int) row1.getCell(1).getNumericCellValue());
            assertEquals(1001L, (long) row1.getCell(2).getNumericCellValue());
            assertEquals(450000.50, row1.getCell(3).getNumericCellValue(), 0.01);
            assertTrue(row1.getCell(4).getBooleanCellValue());
        }
    }

    @Test
    @DisplayName("null値の書き込み - null値を含むデータを正しく書き込めること")
    void testBuilderWithNullValues() throws IOException {
        Path outputPath = Paths.get("src/test/resources/excel_builder_null_test.xlsx");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("田中太郎", null, null, "東京"));

        ExcelWriterWrapper.builder(Person.class, outputPath)
            .write(persons);

        assertTrue(Files.exists(outputPath));

        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Row row1 = sheet.getRow(1);
            assertEquals("田中太郎", getCellValueAsString(row1.getCell(0)));
        }
    }

    @Test
    @DisplayName("複雑なシナリオ - 複数の設定を組み合わせて書き込み、読み込みできること")
    void testBuilderComplexScenario() throws IOException {
        Path outputPath = Paths.get("src/test/resources/excel_builder_complex_test.xlsx");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("複雑太郎", 35, "マネージャー", "福岡"));
        persons.add(new Person("複雑花子", 28, "リーダー", "広島"));
        persons.add(new Person("複雑次郎", 42, "ディレクター", "仙台"));

        ExcelWriterWrapper.builder(Person.class, outputPath)
            .sheetName("複雑なデータ")
            .useHeaderMapping()
            .write(persons);

        assertTrue(Files.exists(outputPath));

        // 読み込んで検証
        List<Person> readPersons = ExcelStreamReader.of(Person.class, outputPath)
            .sheetName("複雑なデータ")
            .process(stream -> stream.toList());

        assertEquals(3, readPersons.size());
        assertEquals("複雑太郎", readPersons.get(0).getName());
        assertEquals(35, readPersons.get(0).getAge());
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
}
