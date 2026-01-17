package com.example.excel.reader;

import com.example.exception.HeaderNotFoundException;
import com.example.exception.SheetNotFoundException;
import com.example.model.Person;
import com.example.model.PersonWithCustomConverter;
import com.example.model.PersonWithoutHeader;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

@DisplayName("ExcelReader: 一括読み込み")
public class ExcelReaderTest {

    private static final Path TEST_RESOURCES_DIR = Paths.get("build/test-outputs/test-resources");
    private static final Path SAMPLE_EXCEL = TEST_RESOURCES_DIR.resolve("sample.xlsx");
    private static final Path SAMPLE_EXCEL_NO_HEADER = TEST_RESOURCES_DIR.resolve("sample_no_header.xlsx");
    private static final Path SAMPLE_EXCEL_MULTI_SHEET = TEST_RESOURCES_DIR.resolve("sample_multi_sheet.xlsx");
    private static final Path SAMPLE_EXCEL_WITH_TITLE = TEST_RESOURCES_DIR.resolve("sample_with_title.xlsx");
    private static final Path SAMPLE_EXCEL_WITH_EMPTY_KEY = TEST_RESOURCES_DIR.resolve("sample_with_empty_key.xlsx");
    private static final Path SAMPLE_EXCEL_HEADER_AT_A1 = TEST_RESOURCES_DIR.resolve("sample_header_at_a1.xlsx");
    private static final Path SAMPLE_EXCEL_HEADER_START_B = TEST_RESOURCES_DIR.resolve("sample_header_start_b.xlsx");
    private static final Path SAMPLE_EXCEL_SPARSE_ROW = TEST_RESOURCES_DIR.resolve("sample_sparse_row.xlsx");
    private static final Path SAMPLE_EXCEL_CUSTOM_CONVERTER = TEST_RESOURCES_DIR.resolve("sample_custom_converter.xlsx");
    private static final Path SAMPLE_EXCEL_FULLWIDTH = TEST_RESOURCES_DIR.resolve("sample_fullwidth.xlsx");

    @BeforeAll
    static void setUp() throws IOException {
        // テストリソース出力ディレクトリを作成（リポジトリ配下に書かない）
        Files.createDirectories(TEST_RESOURCES_DIR);

        // 基本的なサンプルExcelファイルを作成
        createSampleExcel();

        // ヘッダーなしのサンプルExcelファイルを作成
        createSampleExcelNoHeader();

        // 複数シートのサンプルExcelファイルを作成
        createSampleExcelMultiSheet();

        // タイトル付きサンプルExcelファイルを作成（ヘッダーが3行目）
        createSampleExcelWithTitle();

        // キー列が途中で空になるサンプルExcelファイルを作成
        createSampleExcelWithEmptyKey();

        // A1にヘッダー行があるサンプルExcelファイルを作成
        createSampleExcelHeaderAtA1();

        // A列が空でB列からヘッダーが始まるサンプルExcelファイルを作成
        createSampleExcelHeaderStartAtB();

        // 行内に空セルが混在するサンプルExcelファイルを作成
        createSampleExcelWithSparseRow();

        // カスタムコンバーター検証用のサンプルExcelファイルを作成（ヘッダーベース）
        createSampleExcelWithCustomConverter();

        // 全角英数字を含むサンプルExcelファイルを作成
        createSampleExcelWithFullwidth();
    }

    private static void createSampleExcel() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行を作成
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行を作成
            createDataRow(sheet, 1, "田中太郎", 25, "エンジニア", "東京");
            createDataRow(sheet, 2, "佐藤花子", 30, "デザイナー", "大阪");
            createDataRow(sheet, 3, "山田次郎", 28, "営業", "福岡");
            createDataRow(sheet, 4, "高橋健太", 35, "マネージャー", "名古屋");
            createDataRow(sheet, 5, "伊藤美咲", 22, "デザイナー", "横浜");

            workbook.write(fos);
        }
    }

    private static void createDataRow(Sheet sheet, int rowIndex, String name, int age, String occupation, String birthplace) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(age);
        row.createCell(2).setCellValue(occupation);
        row.createCell(3).setCellValue(birthplace);
    }

    private static void createSampleExcelNoHeader() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_NO_HEADER.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダーなしでデータ行を作成
            createDataRow(sheet, 0, "田中太郎", 25, "エンジニア", "東京");
            createDataRow(sheet, 1, "佐藤花子", 30, "デザイナー", "大阪");
            createDataRow(sheet, 2, "山田次郎", 28, "営業", "福岡");
            createDataRow(sheet, 3, "高橋健太", 35, "マネージャー", "名古屋");
            createDataRow(sheet, 4, "伊藤美咲", 22, "デザイナー", "横浜");

            workbook.write(fos);
        }
    }

    private static void createSampleExcelMultiSheet() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_MULTI_SHEET.toFile())) {

            // Sheet1
            Sheet sheet1 = workbook.createSheet("データ1");
            Row headerRow1 = sheet1.createRow(0);
            headerRow1.createCell(0).setCellValue("名前");
            headerRow1.createCell(1).setCellValue("年齢");
            headerRow1.createCell(2).setCellValue("職業");
            headerRow1.createCell(3).setCellValue("出身地");
            createDataRow(sheet1, 1, "田中太郎", 25, "エンジニア", "東京");
            createDataRow(sheet1, 2, "佐藤花子", 30, "デザイナー", "大阪");

            // Sheet2
            Sheet sheet2 = workbook.createSheet("データ2");
            Row headerRow2 = sheet2.createRow(0);
            headerRow2.createCell(0).setCellValue("名前");
            headerRow2.createCell(1).setCellValue("年齢");
            headerRow2.createCell(2).setCellValue("職業");
            headerRow2.createCell(3).setCellValue("出身地");
            createDataRow(sheet2, 1, "山田次郎", 28, "営業", "福岡");
            createDataRow(sheet2, 2, "高橋健太", 35, "マネージャー", "名古屋");
            createDataRow(sheet2, 3, "伊藤美咲", 22, "デザイナー", "横浜");

            workbook.write(fos);
        }
    }

    private static void createSampleExcelWithTitle() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_WITH_TITLE.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // タイトル行（1行目）
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("社員一覧");

            // 空行（2行目）
            sheet.createRow(1);

            // ヘッダー行（3行目）
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行（4行目以降）
            createDataRow(sheet, 3, "田中太郎", 25, "エンジニア", "東京");
            createDataRow(sheet, 4, "佐藤花子", 30, "デザイナー", "大阪");
            createDataRow(sheet, 5, "山田次郎", 28, "営業", "福岡");

            workbook.write(fos);
        }
    }

    private static void createSampleExcelWithEmptyKey() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_WITH_EMPTY_KEY.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行（キー列「名前」が途中で空になる）
            createDataRow(sheet, 1, "田中太郎", 25, "エンジニア", "東京");
            createDataRow(sheet, 2, "佐藤花子", 30, "デザイナー", "大阪");
            createDataRow(sheet, 3, "", 0, "", ""); // 空行
            createDataRow(sheet, 4, "山田次郎", 28, "営業", "福岡"); // この行は読み込まれない

            workbook.write(fos);
        }
    }

    private static void createSampleExcelHeaderAtA1() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_HEADER_AT_A1.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行（1行目、A1にキー項目「名前」がある）
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行（2行目以降）
            createDataRow(sheet, 1, "田中太郎", 25, "エンジニア", "東京");
            createDataRow(sheet, 2, "佐藤花子", 30, "デザイナー", "大阪");
            createDataRow(sheet, 3, "山田次郎", 28, "営業", "福岡");

            workbook.write(fos);
        }
    }

    private static void createSampleExcelHeaderStartAtB() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_HEADER_START_B.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // A列は空、B列からヘッダー
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(1).setCellValue("名前");
            headerRow.createCell(2).setCellValue("年齢");
            headerRow.createCell(3).setCellValue("職業");
            headerRow.createCell(4).setCellValue("出身地");

            // データ行（B列から）
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(1).setCellValue("田中太郎");
            dataRow1.createCell(2).setCellValue(25);
            dataRow1.createCell(3).setCellValue("エンジニア");
            dataRow1.createCell(4).setCellValue("東京");

            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(1).setCellValue("佐藤花子");
            dataRow2.createCell(2).setCellValue(30);
            dataRow2.createCell(3).setCellValue("デザイナー");
            dataRow2.createCell(4).setCellValue("大阪");

            workbook.write(fos);
        }
    }

    private static void createSampleExcelWithSparseRow() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_SPARSE_ROW.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // [null, whitespace, null, data] のパターン
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(1).setCellValue("   ");
            dataRow.createCell(3).setCellValue("東京");

            workbook.write(fos);
        }
    }

    private static void createSampleExcelWithCustomConverter() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_CUSTOM_CONVERTER.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");

            // データ行1（年齢に前後空白、職業は小文字）
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("田中太郎");
            dataRow1.createCell(1).setCellValue(" 25 ");
            dataRow1.createCell(2).setCellValue("engineer");

            // データ行2（年齢が文字列、職業は小文字＋前後空白）
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("佐藤花子");
            dataRow2.createCell(1).setCellValue("30");
            dataRow2.createCell(2).setCellValue(" designer ");

            workbook.write(fos);
        }
    }

    @Test
    @DisplayName("基本的な読み込み - Excelファイルを一括で読み込めること")
    void testBasicRead() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL)
            .read();

        assertNotNull(result);
        assertEquals(5, result.size());

        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
    }

    @Test
    @DisplayName("行スキップ - skip()メソッドで指定行数をスキップできること")
    void testReadWithSkip() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL)
            .skip(2)
            .read();

        assertNotNull(result);
        assertEquals(3, result.size());

        // 最初のPersonの確認（3番目のデータ）
        Person firstPerson = result.get(0);
        assertEquals("山田次郎", firstPerson.getName());
    }

    @Test
    @DisplayName("シート指定 - sheetIndex()メソッドで特定のシートを読み込めること")
    void testReadWithSheetIndex() throws IOException {
        // Sheet1（インデックス0）を読み込む
        List<Person> result1 = ExcelReader.builder(Person.class, SAMPLE_EXCEL_MULTI_SHEET)
            .sheetIndex(0)
            .read();

        assertNotNull(result1);
        assertEquals(2, result1.size());
        assertEquals("田中太郎", result1.get(0).getName());

        // Sheet2（インデックス1）を読み込む
        List<Person> result2 = ExcelReader.builder(Person.class, SAMPLE_EXCEL_MULTI_SHEET)
            .sheetIndex(1)
            .read();

        assertNotNull(result2);
        assertEquals(3, result2.size());
        assertEquals("山田次郎", result2.get(0).getName());
    }

    @Test
    @DisplayName("シート名指定 - sheetName()メソッドで特定のシートを読み込めること")
    void testReadWithSheetName() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL_MULTI_SHEET)
            .sheetName("データ2")
            .read();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("山田次郎", result.get(0).getName());
    }

    @Test
    @DisplayName("位置ベースマッピング - usePositionMapping()でヘッダーなしExcelを読み込めること")
    void testReadWithPositionMapping() throws IOException {
        List<PersonWithoutHeader> result = ExcelReader.builder(PersonWithoutHeader.class, SAMPLE_EXCEL_NO_HEADER)
            .usePositionMapping()
            .read();

        assertNotNull(result);
        assertEquals(5, result.size());

        // 最初のPersonWithoutHeaderの確認
        PersonWithoutHeader firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("ヘッダーマッピング - useHeaderMapping()でヘッダー付きExcelを読み込めること")
    void testReadWithHeaderMapping() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL)
            .useHeaderMapping()
            .read();

        assertNotNull(result);
        assertEquals(5, result.size());

        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
    }

    @Test
    @DisplayName("ヘッダー自動検出 - headerKey()でヘッダー行を自動検出できること（A1にキー項目がない場合）")
    void testReadWithHeaderKey() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL_WITH_TITLE)
            .headerKey("名前")
            .headerSearchRows(5)
            .read();

        assertNotNull(result);
        assertEquals(3, result.size());

        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
    }

    @Test
    @DisplayName("ヘッダー自動検出 - headerKey()でヘッダー行を自動検出できること（A1にキー項目がある場合）")
    void testReadWithHeaderKeyAtA1() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL_HEADER_AT_A1)
            .headerKey("名前")
            .headerSearchRows(5)
            .read();

        assertNotNull(result);
        assertEquals(3, result.size());

        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());

        // 2番目のPersonの確認
        Person secondPerson = result.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
    @DisplayName("ヘッダー自動検出 - A列が空でB列から始まるヘッダーでも読み込めること")
    void testReadWithHeaderKeyStartAtB() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL_HEADER_START_B)
            .headerKey("名前")
            .headerSearchRows(5)
            .read();

        assertNotNull(result);
        assertEquals(2, result.size());

        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
    }

    @Test
    @DisplayName("空セルが混在する行でも空行扱いされず読み込めること")
    void testReadWithSparseRow() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL_SPARSE_ROW)
            .read();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("東京", result.get(0).getBirthplace());
    }

    @Test
    @DisplayName("キー列による終了判定 - headerKey()でキー列が空になったら読み込みを終了すること")
    void testReadWithEmptyKey() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL_WITH_EMPTY_KEY)
            .headerKey("名前")
            .read();

        assertNotNull(result);
        assertEquals(2, result.size()); // 空行の前まで読み込まれる

        assertEquals("田中太郎", result.get(0).getName());
        assertEquals("佐藤花子", result.get(1).getName());
    }

    @Test
    @DisplayName("メソッドチェーン - 複数の設定を組み合わせて使用できること")
    void testReadWithChainedSettings() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL)
            .skip(1)
            .sheetIndex(0)
            .useHeaderMapping()
            .read();

        assertNotNull(result);
        assertEquals(4, result.size()); // 1行スキップして4件

        assertEquals("佐藤花子", result.get(0).getName());
    }

    @Test
    @DisplayName("複数ファイル読み込み - 複数ファイルを順番に読み込めること")
    void testReadMultipleFiles() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, Arrays.asList(SAMPLE_EXCEL, SAMPLE_EXCEL))
            .read();

        assertNotNull(result);
        assertEquals(10, result.size()); // 2ファイル × 5件 = 10件

        // 最初のファイルの最初のデータ
        assertEquals("田中太郎", result.get(0).getName());
        // 2番目のファイルの最初のデータ
        assertEquals("田中太郎", result.get(5).getName());
    }

    @Test
    @DisplayName("シートが見つからない場合 - SheetNotFoundExceptionが投げられること")
    void testSheetNotFound() {
        assertThrows(SheetNotFoundException.class, () -> {
            ExcelReader.builder(Person.class, SAMPLE_EXCEL)
                .sheetIndex(999)
                .read();
        });
    }

    @Test
    @DisplayName("シート名が見つからない場合 - SheetNotFoundExceptionが投げられること")
    void testSheetNameNotFound() {
        assertThrows(SheetNotFoundException.class, () -> {
            ExcelReader.builder(Person.class, SAMPLE_EXCEL)
                .sheetName("存在しないシート")
                .read();
        });
    }

    @Test
    @DisplayName("ヘッダーが見つからない場合 - HeaderNotFoundExceptionが投げられること")
    void testHeaderNotFound() {
        assertThrows(HeaderNotFoundException.class, () -> {
            ExcelReader.builder(Person.class, SAMPLE_EXCEL_NO_HEADER)
                .headerKey("名前")
                .headerSearchRows(5)
                .read();
        });
    }

    @Test
    @DisplayName("キー列が見つからない場合 - HeaderNotFoundExceptionが投げられること")
    void testKeyColumnNotFound() {
        // 注意: headerKey()の動作は「キー列名がセルの値と一致する行を探す」ため、
        // 「存在しない列」という値を持つセルがない場合はHeaderNotFoundExceptionが投げられる。
        // KeyColumnNotFoundExceptionは、ヘッダー行は見つかったが、そのヘッダー行に
        // キー列名が列名として存在しない場合に投げられるが、現在の実装では
        // このケースは発生しない（headerKey()で見つけた行にキー列名が存在するため）。
        // 
        // ExcelStreamReaderTest.testKeyColumnNotInHeader()と同様の動作をテストする。
        HeaderNotFoundException exception = assertThrows(HeaderNotFoundException.class, () -> {
            ExcelReader.builder(Person.class, SAMPLE_EXCEL)
                .headerKey("存在しない列")
                .read();
        });
        
        assertTrue(exception.getMessage().contains("存在しない列"));
    }

    @Test
    @DisplayName("ファイルが存在しない場合 - IOExceptionが投げられること")
    void testFileNotFound() {
        assertThrows(IOException.class, () -> {
            ExcelReader.builder(Person.class, Paths.get("存在しないファイル.xlsx"))
                .read();
        });
    }

    @Test
    @DisplayName("空のファイルリスト - IllegalArgumentExceptionが投げられること")
    void testEmptyFileList() {
        assertThrows(IllegalArgumentException.class, () -> {
            ExcelReader.builder(Person.class, Arrays.asList());
        });
    }

    @Test
    @DisplayName("nullのファイルリスト - IllegalArgumentExceptionが投げられること")
    void testNullFileList() {
        assertThrows(IllegalArgumentException.class, () -> {
            ExcelReader.builder(Person.class, (List<Path>) null);
        });
    }

    @Test
    @DisplayName("一部のセルが存在しない - キー項目以外のセルが存在しない場合、nullのままになること")
    void testMissingCellsBecomeNull() throws IOException {
        // キー項目（名前）のセルしか作成されていないExcelファイルを作成
        Path missingCellsExcel = TEST_RESOURCES_DIR.resolve("sample_missing_cells.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(missingCellsExcel.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行（全ての列を定義）
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行1（名前のセルしか作成しない）
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("田中太郎");
            // 年齢、職業、出身地のセルは作成しない（null）

            // データ行2（名前と年齢のセルのみ）
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("佐藤花子");
            dataRow2.createCell(1).setCellValue(30);
            // 職業、出身地のセルは作成しない（null）

            // データ行3（全てのセルに値がある）
            createDataRow(sheet, 3, "山田次郎", 28, "営業", "福岡");

            workbook.write(fos);
        }

        List<Person> result = ExcelReader.builder(Person.class, missingCellsExcel)
            .read();

        assertNotNull(result);
        assertEquals(3, result.size());

        // 1行目: 名前のみ設定、他はnull
        Person person1 = result.get(0);
        assertEquals("田中太郎", person1.getName());
        assertNull(person1.getAge(), "年齢のセルが存在しないためnullになる");
        assertNull(person1.getOccupation(), "職業のセルが存在しないためnullになる");
        assertNull(person1.getBirthplace(), "出身地のセルが存在しないためnullになる");

        // 2行目: 名前と年齢のみ設定、他はnull
        Person person2 = result.get(1);
        assertEquals("佐藤花子", person2.getName());
        assertEquals(30, person2.getAge());
        assertNull(person2.getOccupation(), "職業のセルが存在しないためnullになる");
        assertNull(person2.getBirthplace(), "出身地のセルが存在しないためnullになる");

        // 3行目: 全てのフィールドに値がある
        Person person3 = result.get(2);
        assertEquals("山田次郎", person3.getName());
        assertEquals(28, person3.getAge());
        assertEquals("営業", person3.getOccupation());
        assertEquals("福岡", person3.getBirthplace());
    }

    @Test
    @DisplayName("カスタムコンバーター - @CsvCustomBindByName が ExcelReader でも適用されること")
    void testCustomConverterWithHeaderMapping() throws IOException {
        List<PersonWithCustomConverter> result = ExcelReader
            .builder(PersonWithCustomConverter.class, SAMPLE_EXCEL_CUSTOM_CONVERTER)
            .useHeaderMapping()
            .read();

        assertNotNull(result);
        assertEquals(2, result.size());

        PersonWithCustomConverter p1 = result.get(0);
        assertEquals("田中太郎", p1.getName());
        // " 25 " -> 25 に変換されていること
        assertEquals(25, p1.getAge());
        // occupation は @CsvCustomBindByPosition 用なので、このテストでは null のまま
        assertNull(p1.getOccupation());

        PersonWithCustomConverter p2 = result.get(1);
        assertEquals("佐藤花子", p2.getName());
        assertEquals(30, p2.getAge());
        assertNull(p2.getOccupation());
    }

    private static void createSampleExcelWithFullwidth() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_FULLWIDTH.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行1（全角数字を含む）
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("田中太郎");
            dataRow1.createCell(1).setCellValue(25);
            dataRow1.createCell(2).setCellValue("エンジニア");
            dataRow1.createCell(3).setCellValue("東京０１２３"); // 全角数字

            // データ行2（全角英字を含む）
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("佐藤花子");
            dataRow2.createCell(1).setCellValue(30);
            dataRow2.createCell(2).setCellValue("デザイナー");
            dataRow2.createCell(3).setCellValue("大阪ＡＢＣ"); // 全角英字大文字

            // データ行3（全角英数字が混在）
            Row dataRow3 = sheet.createRow(3);
            dataRow3.createCell(0).setCellValue("山田次郎");
            dataRow3.createCell(1).setCellValue(28);
            dataRow3.createCell(2).setCellValue("営業");
            dataRow3.createCell(3).setCellValue("福岡ａｂｃ１２３"); // 全角英字小文字と数字

            // データ行4（全角英数字のみ）
            Row dataRow4 = sheet.createRow(4);
            dataRow4.createCell(0).setCellValue("高橋健太");
            dataRow4.createCell(1).setCellValue(35);
            dataRow4.createCell(2).setCellValue("マネージャー");
            dataRow4.createCell(3).setCellValue("ＴＥＳＴ１２３"); // 全角英数字のみ

            workbook.write(fos);
        }
    }

    @Test
    @DisplayName("全角英数字の保持 - 全角英数字が半角に変換されずに保持されること")
    void testFullwidthCharactersPreserved() throws IOException {
        List<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL_FULLWIDTH)
            .read();

        assertNotNull(result);
        assertEquals(4, result.size());

        // 全角数字が保持されていること
        Person person1 = result.get(0);
        assertEquals("東京０１２３", person1.getBirthplace(), "全角数字が半角に変換されていないこと");

        // 全角英字大文字が保持されていること
        Person person2 = result.get(1);
        assertEquals("大阪ＡＢＣ", person2.getBirthplace(), "全角英字大文字が半角に変換されていないこと");

        // 全角英字小文字と数字が保持されていること
        Person person3 = result.get(2);
        assertEquals("福岡ａｂｃ１２３", person3.getBirthplace(), "全角英字小文字と数字が半角に変換されていないこと");

        // 全角英数字のみが保持されていること
        Person person4 = result.get(3);
        assertEquals("ＴＥＳＴ１２３", person4.getBirthplace(), "全角英数字のみが半角に変換されていないこと");
    }
}

