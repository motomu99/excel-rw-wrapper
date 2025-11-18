package com.example.excel.writer;

import com.example.excel.domain.Anchor;
import com.example.excel.domain.Book;
import com.example.excel.domain.Sheet;
import com.example.excel.domain.Table;
import com.example.model.Order;
import com.example.model.Person;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@DisplayName("BookWriter: DDD設計によるExcel書き込み")
public class BookWriterTest {

    private static final Path TEST_OUTPUT_DIR = Paths.get("build/test-outputs");
    
    @BeforeAll
    static void setUp() throws IOException {
        // テスト出力ディレクトリを作成
        Files.createDirectories(TEST_OUTPUT_DIR);
    }

    @Test
    @DisplayName("基本的なBook書き込み - DDDモデルでExcelファイルに書き込めること")
    void testBasicBookWrite() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("book_write.xlsx");
        
        // Personデータ
        List<Person> persons = List.of(
            new Person("田中太郎", 25, "エンジニア", "東京"),
            new Person("佐藤花子", 30, "デザイナー", "大阪")
        );
        
        // Orderデータ
        List<Order> orders = List.of(
            new Order("O001", "U001", 1200, "2025-01-01"),
            new Order("O002", "U002", 3000, "2025-01-02")
        );
        
        // DDDモデルを構築
        Book book = Book.of(outputPath)
            .addSheet(Sheet.of("Report")
                .addTable(Table.builder(Person.class)
                    .anchor("A1")
                    .data(persons)
                    .build())
                .addTable(Table.builder(Order.class)
                    .anchor("A10")
                    .data(orders)
                    .build()));
        
        // 書き込み
        BookWriter.write(book);
        
        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));
        
        // ファイルの内容を確認
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet("Report");
            assertNotNull(sheet);
            
            // 1つ目のテーブル（Person）の確認
            // ヘッダー行（@CsvBindByNameアノテーションから自動抽出）
            Row headerRow1 = sheet.getRow(0);
            assertNotNull(headerRow1);
            assertEquals("名前", getCellValueAsString(headerRow1.getCell(0)));
            assertEquals("年齢", getCellValueAsString(headerRow1.getCell(1)));
            assertEquals("職業", getCellValueAsString(headerRow1.getCell(2)));
            assertEquals("出身地", getCellValueAsString(headerRow1.getCell(3)));
            
            // データ行
            Row dataRow1 = sheet.getRow(1);
            assertEquals("田中太郎", getCellValueAsString(dataRow1.getCell(0)));
            assertEquals(25, getCellValueAsNumber(dataRow1.getCell(1)));
            assertEquals("エンジニア", getCellValueAsString(dataRow1.getCell(2)));
            assertEquals("東京", getCellValueAsString(dataRow1.getCell(3)));
            
            Row dataRow2 = sheet.getRow(2);
            assertEquals("佐藤花子", getCellValueAsString(dataRow2.getCell(0)));
            assertEquals(30, getCellValueAsNumber(dataRow2.getCell(1)));
            assertEquals("デザイナー", getCellValueAsString(dataRow2.getCell(2)));
            assertEquals("大阪", getCellValueAsString(dataRow2.getCell(3)));
            
            // 2つ目のテーブル（Order）の確認
            // ヘッダー行（@CsvBindByNameアノテーションから自動抽出）
            Row headerRow2 = sheet.getRow(9);
            assertNotNull(headerRow2);
            assertEquals("注文ID", getCellValueAsString(headerRow2.getCell(0)));
            assertEquals("ユーザーID", getCellValueAsString(headerRow2.getCell(1)));
            assertEquals("金額", getCellValueAsString(headerRow2.getCell(2)));
            assertEquals("注文日", getCellValueAsString(headerRow2.getCell(3)));
            
            // データ行
            Row orderRow1 = sheet.getRow(10);
            assertEquals("O001", getCellValueAsString(orderRow1.getCell(0)));
            assertEquals("U001", getCellValueAsString(orderRow1.getCell(1)));
            assertEquals(1200, getCellValueAsNumber(orderRow1.getCell(2)));
            assertEquals("2025-01-01", getCellValueAsString(orderRow1.getCell(3)));
            
            Row orderRow2 = sheet.getRow(11);
            assertEquals("O002", getCellValueAsString(orderRow2.getCell(0)));
            assertEquals("U002", getCellValueAsString(orderRow2.getCell(1)));
            assertEquals(3000, getCellValueAsNumber(orderRow2.getCell(2)));
            assertEquals("2025-01-02", getCellValueAsString(orderRow2.getCell(3)));
        }
    }
    
    @Test
    @DisplayName("複数シートの書き込み - 複数のシートが正しく作成されること")
    void testMultipleSheets() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("multiple_sheets.xlsx");
        
        List<Person> persons = List.of(
            new Person("山田次郎", 28, "営業", "福岡")
        );
        
        List<Order> orders = List.of(
            new Order("O001", "U001", 1200, "2025-01-01")
        );
        
        // 複数シートのBookを構築
        Book book = Book.of(outputPath)
            .addSheet(Sheet.of("Users")
                .addTable(Table.builder(Person.class)
                    .anchor("A1")
                    .data(persons)
                    .build()))
            .addSheet(Sheet.of("Orders")
                .addTable(Table.builder(Order.class)
                    .anchor("A1")
                    .data(orders)
                    .build()));
        
        BookWriter.write(book);
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            // Usersシートの確認
            org.apache.poi.ss.usermodel.Sheet usersSheet = workbook.getSheet("Users");
            assertNotNull(usersSheet);
            assertEquals("名前", getCellValueAsString(usersSheet.getRow(0).getCell(0)));
            assertEquals("山田次郎", getCellValueAsString(usersSheet.getRow(1).getCell(0)));
            
            // Ordersシートの確認
            org.apache.poi.ss.usermodel.Sheet ordersSheet = workbook.getSheet("Orders");
            assertNotNull(ordersSheet);
            assertEquals("注文ID", getCellValueAsString(ordersSheet.getRow(0).getCell(0)));
            assertEquals("O001", getCellValueAsString(ordersSheet.getRow(1).getCell(0)));
            
            // シート数が2つであることを確認
            assertEquals(2, workbook.getNumberOfSheets());
        }
    }
    
    @Test
    @DisplayName("Anchor値オブジェクトの使用 - Anchor.of()で正しく位置を指定できること")
    void testAnchorValueObject() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("anchor_value_object.xlsx");
        
        List<Person> persons = List.of(
            new Person("テスト太郎", 25, "エンジニア", "東京")
        );
        
        // Anchor値オブジェクトを使用
        Anchor anchor = Anchor.of("B5");
        
        Book book = Book.of(outputPath)
            .addSheet(Sheet.of("Test")
                .addTable(Table.builder(Person.class)
                    .anchor(anchor)
                    .data(persons)
                    .build()));
        
        BookWriter.write(book);
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet("Test");
            
            // B5セル（行4、列1）から開始されることを確認
            Row headerRow = sheet.getRow(4);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(1)));
            
            Row dataRow = sheet.getRow(5);
            assertEquals("テスト太郎", getCellValueAsString(dataRow.getCell(1)));
        }
    }
    
    @Test
    @DisplayName("空のデータテーブル - データが空でもヘッダーは書き込まれること")
    void testEmptyDataTable() throws IOException {
        Path outputPath = TEST_OUTPUT_DIR.resolve("empty_data_table.xlsx");
        
        Book book = Book.of(outputPath)
            .addSheet(Sheet.of("Test")
                .addTable(Table.builder(Person.class)
                    .anchor("A1")
                    .data(List.of())
                    .build()));
        
        BookWriter.write(book);
        
        assertTrue(Files.exists(outputPath));
        
        try (FileInputStream fis = new FileInputStream(outputPath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheet("Test");
            
            // ヘッダー行（@CsvBindByNameアノテーションから自動抽出）
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);
            assertEquals("名前", getCellValueAsString(headerRow.getCell(0)));
            assertEquals("年齢", getCellValueAsString(headerRow.getCell(1)));
            assertEquals("職業", getCellValueAsString(headerRow.getCell(2)));
            assertEquals("出身地", getCellValueAsString(headerRow.getCell(3)));
            
            // データ行がないことを確認
            assertEquals(1, sheet.getPhysicalNumberOfRows()); // ヘッダーのみ
        }
    }
    
    @Test
    @DisplayName("Tableの不変性 - Tableオブジェクトが不変であること")
    void testTableImmutability() {
        List<Person> persons = List.of(
            new Person("テスト太郎", 25, "エンジニア", "東京")
        );
        
        Table<Person> table = Table.builder(Person.class)
            .anchor("A1")
            .data(persons)
            .build();
        
        // データリストが不変であることを確認
        List<Person> data = table.getData();
        assertThrows(UnsupportedOperationException.class, () -> {
            data.add(new Person("追加", 30, "デザイナー", "大阪"));
        });
    }
    
    @Test
    @DisplayName("Sheetの不変性 - Sheetオブジェクトが不変であること")
    void testSheetImmutability() {
        Table<Person> table = Table.builder(Person.class)
            .anchor("A1")
            .data(List.of())
            .build();
        
        Sheet sheet = Sheet.of("Test")
            .addTable(table);
        
        // テーブルリストが不変であることを確認
        List<Table<?>> tables = sheet.getTables();
        assertThrows(UnsupportedOperationException.class, () -> {
            tables.add(table);
        });
    }
    
    /**
     * セルの値を文字列として取得
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return cell.toString();
        }
    }
    
    /**
     * セルの値を数値として取得
     */
    private double getCellValueAsNumber(Cell cell) {
        if (cell == null) {
            return 0;
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    return 0;
                }
            default:
                return 0;
        }
    }
}

