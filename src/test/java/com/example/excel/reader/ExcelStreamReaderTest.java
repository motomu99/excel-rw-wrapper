package com.example.excel.reader;

import com.example.exception.CellValueConversionException;
import com.example.exception.HeaderNotFoundException;
import com.example.exception.SheetNotFoundException;
import com.example.model.Person;
import com.example.model.PersonWithAllTypes;
import com.example.model.PersonWithCustomConverter;
import com.example.model.PersonWithDate;
import com.example.model.PersonWithoutHeader;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DisplayName("ExcelStreamReader: Stream APIを使用したExcel読み込み")
public class ExcelStreamReaderTest {

    private static final Path TEST_RESOURCES_DIR = Paths.get("build/test-outputs/test-resources");
    private static final Path SAMPLE_EXCEL = TEST_RESOURCES_DIR.resolve("sample.xlsx");
    private static final Path SAMPLE_EXCEL_NO_HEADER = TEST_RESOURCES_DIR.resolve("sample_no_header.xlsx");
    private static final Path SAMPLE_EXCEL_MULTI_SHEET = TEST_RESOURCES_DIR.resolve("sample_multi_sheet.xlsx");
    private static final Path SAMPLE_EXCEL_WITH_TITLE = TEST_RESOURCES_DIR.resolve("sample_with_title.xlsx");
    private static final Path SAMPLE_EXCEL_WITH_EMPTY_KEY = TEST_RESOURCES_DIR.resolve("sample_with_empty_key.xlsx");
    private static final Path SAMPLE_EXCEL_INVALID_TYPE = TEST_RESOURCES_DIR.resolve("sample_invalid_type.xlsx");
    private static final Path SAMPLE_EXCEL_ALL_TYPES = TEST_RESOURCES_DIR.resolve("sample_all_types.xlsx");
    private static final Path SAMPLE_EXCEL_WITH_DATE = TEST_RESOURCES_DIR.resolve("sample_with_date.xlsx");
    private static final Path SAMPLE_EXCEL_WITH_FORMULA = TEST_RESOURCES_DIR.resolve("sample_with_formula.xlsx");
    private static final Path SAMPLE_EXCEL_CUSTOM_CONVERTER = TEST_RESOURCES_DIR.resolve("sample_custom_converter.xlsx");
    private static final Path SAMPLE_EXCEL_CUSTOM_CONVERTER_POSITION = TEST_RESOURCES_DIR.resolve("sample_custom_converter_position.xlsx");
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

        // 型変換エラー用のサンプルExcelファイルを作成
        createSampleExcelInvalidType();

        // 全型テスト用のサンプルExcelファイルを作成
        createSampleExcelAllTypes();

        // 日付型テスト用のサンプルExcelファイルを作成
        createSampleExcelWithDate();

        // 数式セルテスト用のサンプルExcelファイルを作成
        createSampleExcelWithFormula();

        // カスタムコンバーター検証用のサンプルExcelファイルを作成（ヘッダーベース）
        createSampleExcelWithCustomConverter();

        // カスタムコンバーター検証用のサンプルExcelファイルを作成（位置ベース）
        createSampleExcelWithCustomConverterPosition();

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
            createDataRow(sheet, 5, "伊藤美咲", 27, "エンジニア", "札幌");

            workbook.write(fos);
        }
    }

    private static void createSampleExcelNoHeader() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_NO_HEADER.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // データ行を作成（名前と年齢のみ）
            createSimpleDataRow(sheet, 0, "田中太郎", 25);
            createSimpleDataRow(sheet, 1, "佐藤花子", 30);
            createSimpleDataRow(sheet, 2, "山田次郎", 28);
            createSimpleDataRow(sheet, 3, "高橋健太", 35);
            createSimpleDataRow(sheet, 4, "伊藤美咲", 27);

            workbook.write(fos);
        }
    }

    private static void createSampleExcelMultiSheet() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_MULTI_SHEET.toFile())) {

            // Sheet1を作成
            Sheet sheet1 = workbook.createSheet("データ1");
            Row headerRow1 = sheet1.createRow(0);
            headerRow1.createCell(0).setCellValue("名前");
            headerRow1.createCell(1).setCellValue("年齢");
            headerRow1.createCell(2).setCellValue("職業");
            headerRow1.createCell(3).setCellValue("出身地");
            createDataRow(sheet1, 1, "田中太郎", 25, "エンジニア", "東京");
            createDataRow(sheet1, 2, "佐藤花子", 30, "デザイナー", "大阪");

            // Sheet2を作成
            Sheet sheet2 = workbook.createSheet("データ2");
            Row headerRow2 = sheet2.createRow(0);
            headerRow2.createCell(0).setCellValue("名前");
            headerRow2.createCell(1).setCellValue("年齢");
            headerRow2.createCell(2).setCellValue("職業");
            headerRow2.createCell(3).setCellValue("出身地");
            createDataRow(sheet2, 1, "山田次郎", 28, "営業", "福岡");
            createDataRow(sheet2, 2, "高橋健太", 35, "マネージャー", "名古屋");
            createDataRow(sheet2, 3, "伊藤美咲", 27, "エンジニア", "札幌");

            workbook.write(fos);
        }
    }

    private static void createDataRow(Sheet sheet, int rowNum, String name, int age, String occupation, String birthplace) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(age);
        row.createCell(2).setCellValue(occupation);
        row.createCell(3).setCellValue(birthplace);
    }

    private static void createSimpleDataRow(Sheet sheet, int rowNum, String name, int age) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(name);
        row.createCell(1).setCellValue(age);
    }

    private static void createSampleExcelWithTitle() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_WITH_TITLE.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // タイトル行（0行目）
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("社員マスタ");

            // 説明行（1行目）
            Row descRow = sheet.createRow(1);
            descRow.createCell(0).setCellValue("2024年度版");

            // ヘッダー行（2行目）
            Row headerRow = sheet.createRow(2);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行を作成（3行目から）
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

            // データ行（3件のデータ）
            createDataRow(sheet, 1, "田中太郎", 25, "エンジニア", "東京");
            createDataRow(sheet, 2, "佐藤花子", 30, "デザイナー", "大阪");
            createDataRow(sheet, 3, "山田次郎", 28, "営業", "福岡");

            // 名前が空の行（この行で読み込みが終了するはず）
            Row emptyKeyRow = sheet.createRow(4);
            emptyKeyRow.createCell(0).setCellValue(""); // 名前が空
            emptyKeyRow.createCell(1).setCellValue(99);
            emptyKeyRow.createCell(2).setCellValue("テスト");
            emptyKeyRow.createCell(3).setCellValue("テスト");

            // その後のデータ（読み込まれないはず）
            createDataRow(sheet, 5, "高橋健太", 35, "マネージャー", "名古屋");
            createDataRow(sheet, 6, "伊藤美咲", 27, "エンジニア", "札幌");

            workbook.write(fos);
        }
    }

    private static void createSampleExcelInvalidType() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_INVALID_TYPE.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行（1件目は正常）
            createDataRow(sheet, 1, "田中太郎", 25, "エンジニア", "東京");

            // データ行（2件目は年齢が文字列）
            Row invalidRow = sheet.createRow(2);
            invalidRow.createCell(0).setCellValue("佐藤花子");
            invalidRow.createCell(1).setCellValue("abc"); // 年齢に文字列
            invalidRow.createCell(2).setCellValue("デザイナー");
            invalidRow.createCell(3).setCellValue("大阪");

            workbook.write(fos);
        }
    }

    @Test
    @DisplayName("基本的なStream処理 - ExcelファイルをStreamとして読み込み、Listに変換できること")
    void testBasicStreamProcessing() throws IOException {
        List<Person> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(5, result.size()); // ヘッダーを除いた5件のデータ

        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
    }

    @Test
    @DisplayName("フィルタリング - Stream.filter()を使用してデータを絞り込めること")
    void testStreamWithFiltering() throws IOException {
        List<Person> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL)
            .extract(stream -> stream
                .filter(person -> person.getAge() >= 30)
                .collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(2, result.size()); // 30歳以上の人は2人

        // 年齢の確認
        assertTrue(result.stream().allMatch(person -> person.getAge() >= 30));
    }

    @Test
    @DisplayName("マッピング - Stream.map()を使用してデータを変換できること")
    void testStreamWithMapping() throws IOException {
        List<String> names = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL)
            .extract(stream -> stream
                .map(Person::getName)
                .collect(Collectors.toList()));

        assertNotNull(names);
        assertEquals(5, names.size());
        assertTrue(names.contains("田中太郎"));
        assertTrue(names.contains("佐藤花子"));
    }

    @Test
    @DisplayName("行スキップ - skip()メソッドで指定行数をスキップできること")
    void testStreamWithSkip() throws IOException {
        List<Person> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL)
            .skip(2)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size()); // 2行スキップして3件

        // 最初のPersonの確認（3番目のデータ）
        Person firstPerson = result.get(0);
        assertEquals("山田次郎", firstPerson.getName());
    }

    @Test
    @DisplayName("シート指定 - sheetIndex()メソッドで特定のシートを読み込めること")
    void testStreamWithSheetIndex() throws IOException {
        // Sheet1（インデックス0）を読み込む
        List<Person> result1 = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_MULTI_SHEET)
            .sheetIndex(0)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result1);
        assertEquals(2, result1.size());
        assertEquals("田中太郎", result1.get(0).getName());

        // Sheet2（インデックス1）を読み込む
        List<Person> result2 = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_MULTI_SHEET)
            .sheetIndex(1)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result2);
        assertEquals(3, result2.size());
        assertEquals("山田次郎", result2.get(0).getName());
    }

    @Test
    @DisplayName("シート名指定 - sheetName()メソッドで特定のシートを読み込めること")
    void testStreamWithSheetName() throws IOException {
        List<Person> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_MULTI_SHEET)
            .sheetName("データ2")
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("山田次郎", result.get(0).getName());
    }

    @Test
    @DisplayName("位置ベースマッピング - usePositionMapping()でヘッダーなしExcelを読み込めること")
    void testStreamWithPositionMapping() throws IOException {
        List<PersonWithoutHeader> result = ExcelStreamReader.builder(PersonWithoutHeader.class, SAMPLE_EXCEL_NO_HEADER)
            .usePositionMapping()
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(5, result.size());

        // 最初のPersonWithoutHeaderの確認
        PersonWithoutHeader firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("ヘッダーマッピング - useHeaderMapping()でヘッダー付きExcelを読み込めること")
    void testStreamWithHeaderMapping() throws IOException {
        List<Person> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL)
            .useHeaderMapping()
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(5, result.size());

        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
    }

    @Test
    @DisplayName("メソッドチェーン - 複数の設定とStream操作を組み合わせて使用できること")
    void testStreamWithChainedOperations() throws IOException {
        List<String> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL)
            .skip(1)
            .sheetIndex(0)
            .useHeaderMapping()
            .extract(stream -> stream
                .filter(person -> person.getAge() >= 25)
                .map(Person::getName)
                .collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(4, result.size()); // 1行スキップして、残り4人全員が25歳以上

        assertTrue(result.contains("佐藤花子"));
        assertTrue(result.contains("山田次郎"));
        assertTrue(result.contains("高橋健太"));
        assertTrue(result.contains("伊藤美咲"));
    }

    @Test
    @DisplayName("カウント操作 - Stream.count()でレコード数を取得できること")
    void testStreamCount() throws IOException {
        Long count = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL)
            .extract(Stream::count);

        assertNotNull(count);
        assertEquals(5L, count);
    }

    @Test
    @DisplayName("forEach操作 - Stream.forEach()で各レコードを処理できること")
    void testStreamForEach() throws IOException {
        StringBuilder names = new StringBuilder();

        ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL)
            .consume(stream -> {
                stream.forEach(person -> names.append(person.getName()).append(","));
            });

        String result = names.toString();
        assertTrue(result.contains("田中太郎"));
        assertTrue(result.contains("佐藤花子"));
    }

    @Test
    @DisplayName("Consumerオーバーロード - 戻り値なしで副作用処理ができること")
    void testConsumerOverloadBasic() throws IOException {
        StringBuilder names = new StringBuilder();

        ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL)
            .consume(stream -> {
                stream.map(Person::getName).forEach(name -> names.append(name).append(","));
            });

        String result = names.toString();
        assertTrue(result.contains("田中太郎"));
        assertTrue(result.contains("佐藤花子"));
        assertTrue(result.contains("山田次郎"));
    }

    @Test
    @DisplayName("複数ファイル読み込み - ファイル順を保って1つのStreamとして連結されること")
    void testMultiFileSequentialStream() throws IOException {
        List<Path> files = Arrays.asList(
            SAMPLE_EXCEL,
            SAMPLE_EXCEL_MULTI_SHEET
        );

        List<Person> persons = ExcelStreamReader.builder(Person.class, files)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(persons);
        assertEquals(7, persons.size()); // 5人 + 2人
        assertEquals("田中太郎", persons.get(0).getName()); // sample.xlsx 1人目
        assertEquals("伊藤美咲", persons.get(4).getName()); // sample.xlsx 5人目
        assertEquals("田中太郎", persons.get(5).getName()); // sample_multi_sheet.xlsx - データ1の1人目
        assertEquals("佐藤花子", persons.get(6).getName()); // sample_multi_sheet.xlsx - データ1の2人目
    }

    @Test
    @DisplayName("複数ファイルスキップ - skip()は各ファイルの先頭行をスキップすること")
    void testSkipPerFileInMultiFileStream() throws IOException {
        List<Person> file1Rows = Arrays.asList(
            new Person("ファイル1-行1", 20, "職1", "地1"),
            new Person("ファイル1-行2", 21, "職2", "地2"),
            new Person("ファイル1-行3", 22, "職3", "地3")
        );
        List<Person> file2Rows = Arrays.asList(
            new Person("ファイル2-行1", 30, "職A", "地A"),
            new Person("ファイル2-行2", 31, "職B", "地B"),
            new Person("ファイル2-行3", 32, "職C", "地C")
        );

        Path file1 = createTempExcel(file1Rows);
        Path file2 = createTempExcel(file2Rows);

        try {
            List<String> names = ExcelStreamReader.builder(Person.class, Arrays.asList(file1, file2))
                .skip(1)
                .extract(stream -> stream
                    .map(Person::getName)
                    .collect(Collectors.toList()));

            assertEquals(4, names.size());
            assertEquals("ファイル1-行2", names.get(0));
            assertEquals("ファイル1-行3", names.get(1));
            assertEquals("ファイル2-行2", names.get(2));
            assertEquals("ファイル2-行3", names.get(3));
        } finally {
            Files.deleteIfExists(file1);
            Files.deleteIfExists(file2);
        }
    }

    @Test
    @DisplayName("ヘッダー自動検出 - headerKey()でヘッダー行を自動検出できること")
    void testHeaderAutoDetection() throws IOException {
        List<Person> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_WITH_TITLE)
            .headerKey("名前")
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size());

        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
    }

    @Test
    @DisplayName("キー列終了判定 - キー列が空になったら読み込みを終了すること")
    void testStopReadingWhenKeyColumnIsEmpty() throws IOException {
        List<Person> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_WITH_EMPTY_KEY)
            .headerKey("名前")
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size()); // 空行の前の3件のみ

        // データの確認
        assertEquals("田中太郎", result.get(0).getName());
        assertEquals("佐藤花子", result.get(1).getName());
        assertEquals("山田次郎", result.get(2).getName());

        // 空行以降のデータは読み込まれていないことを確認
        assertFalse(result.stream().anyMatch(person -> "高橋健太".equals(person.getName())));
        assertFalse(result.stream().anyMatch(person -> "伊藤美咲".equals(person.getName())));
    }

    @Test
    @DisplayName("ヘッダー探索範囲 - headerSearchRows()でヘッダー探索範囲を指定できること")
    void testHeaderSearchRows() throws IOException {
        List<Person> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_WITH_TITLE)
            .headerKey("名前")
            .headerSearchRows(5)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("田中太郎", result.get(0).getName());
    }

    @Test
    @DisplayName("ヘッダー未検出 - ヘッダーが見つからない場合はHeaderNotFoundExceptionを投げること")
    void testHeaderNotFound() {
        HeaderNotFoundException exception = assertThrows(HeaderNotFoundException.class, () -> {
            ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_WITH_TITLE)
                .headerKey("存在しない列名")
                .extract(stream -> stream.collect(Collectors.toList()));
        });

        assertTrue(exception.getMessage().contains("存在しない列名"));
        assertTrue(exception.getMessage().contains("見つかりませんでした"));
    }

    @Test
    @DisplayName("ヘッダー自動検出とStream操作の組み合わせ")
    void testHeaderAutoDetectionWithStreamOperations() throws IOException {
        List<String> names = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_WITH_TITLE)
            .headerKey("名前")
            .extract(stream -> stream
                .filter(person -> person.getAge() >= 28)
                .map(Person::getName)
                .collect(Collectors.toList()));

        assertNotNull(names);
        assertEquals(2, names.size());
        assertTrue(names.contains("佐藤花子"));
        assertTrue(names.contains("山田次郎"));
    }

    @Test
    @DisplayName("キー列がヘッダーに存在しない - 指定したキー列を持つヘッダー行が見つからない場合はHeaderNotFoundExceptionを投げること")
    void testKeyColumnNotInHeader() {
        HeaderNotFoundException exception = assertThrows(HeaderNotFoundException.class, () -> {
            ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL)
                .headerKey("存在しない列")
                .extract(stream -> stream.collect(Collectors.toList()));
        });

        assertTrue(exception.getMessage().contains("存在しない列"));
    }

    @Test
    @DisplayName("型変換エラー - 数字項目に文字列が来た場合はCellValueConversionExceptionを投げること")
    void testCellValueConversionError() {
        CellValueConversionException exception = assertThrows(CellValueConversionException.class, () -> {
            ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_INVALID_TYPE)
                .extract(stream -> stream.collect(Collectors.toList()));
        });

        // エラーメッセージの確認
        assertTrue(exception.getMessage().contains("セル値の変換に失敗しました"));
        assertTrue(exception.getMessage().contains("abc"));
        assertTrue(exception.getMessage().contains("年齢"));
        assertTrue(exception.getMessage().contains("Integer") || exception.getMessage().contains("int"));

        // エラー詳細の確認
        assertEquals(2, exception.getRowIndex()); // 0から始まるので2（3行目）
        assertEquals("年齢", exception.getColumnName());
        assertEquals("abc", exception.getCellValue());
        assertTrue(exception.getTargetType().contains("Integer") || exception.getTargetType().contains("int"));
    }

    private static void createSampleExcelAllTypes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_ALL_TYPES.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("ID");
            headerRow.createCell(3).setCellValue("給料");
            headerRow.createCell(4).setCellValue("有効");

            // データ行1（全て数値型で設定）
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("田中太郎");
            dataRow1.createCell(1).setCellValue(25);
            dataRow1.createCell(2).setCellValue(1001);
            dataRow1.createCell(3).setCellValue(450000.50);
            dataRow1.createCell(4).setCellValue(true);

            // データ行2（文字列からの変換をテスト）
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("佐藤花子");
            dataRow2.createCell(1).setCellValue("30");
            dataRow2.createCell(2).setCellValue("2002");
            dataRow2.createCell(3).setCellValue("550000.75");
            dataRow2.createCell(4).setCellValue("true");

            // データ行3（Boolean false）
            Row dataRow3 = sheet.createRow(3);
            dataRow3.createCell(0).setCellValue("山田次郎");
            dataRow3.createCell(1).setCellValue(28);
            dataRow3.createCell(2).setCellValue(3003);
            dataRow3.createCell(3).setCellValue(500000.0);
            dataRow3.createCell(4).setCellValue(false);

            workbook.write(fos);
        }
    }
    
    private static Path createTempExcel(List<Person> rows) throws IOException {
        Path tempFile = Files.createTempFile("excel-multi-skip", ".xlsx");
        tempFile.toFile().deleteOnExit();

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            Sheet sheet = workbook.createSheet("Sheet1");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("名前");
            header.createCell(1).setCellValue("年齢");
            header.createCell(2).setCellValue("職業");
            header.createCell(3).setCellValue("出身地");

            for (int i = 0; i < rows.size(); i++) {
                Person person = rows.get(i);
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(person.getName());
                if (person.getAge() != null) {
                    row.createCell(1).setCellValue(person.getAge());
                }
                row.createCell(2).setCellValue(person.getOccupation());
                row.createCell(3).setCellValue(person.getBirthplace());
            }

            workbook.write(fos);
        }

        return tempFile;
    }

    @Test
    @DisplayName("Long型変換 - Long型フィールドを正しく読み込めること")
    void testLongTypeConversion() throws IOException {
        List<PersonWithAllTypes> result = ExcelStreamReader.builder(PersonWithAllTypes.class, SAMPLE_EXCEL_ALL_TYPES)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size());

        // 数値セルからの変換
        assertEquals(1001L, result.get(0).getId());
        // 文字列セルからの変換
        assertEquals(2002L, result.get(1).getId());
    }

    @Test
    @DisplayName("Double型変換 - Double型フィールドを正しく読み込めること")
    void testDoubleTypeConversion() throws IOException {
        List<PersonWithAllTypes> result = ExcelStreamReader.builder(PersonWithAllTypes.class, SAMPLE_EXCEL_ALL_TYPES)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size());

        // 数値セルからの変換
        assertEquals(450000.50, result.get(0).getSalary(), 0.01);
        // 文字列セルからの変換
        assertEquals(550000.75, result.get(1).getSalary(), 0.01);
    }

    @Test
    @DisplayName("Boolean型変換 - Boolean型フィールドを正しく読み込めること")
    void testBooleanTypeConversion() throws IOException {
        List<PersonWithAllTypes> result = ExcelStreamReader.builder(PersonWithAllTypes.class, SAMPLE_EXCEL_ALL_TYPES)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size());

        // Booleanセルからの変換（true）
        assertTrue(result.get(0).getActive());
        // 文字列セルからの変換（"true"）
        assertTrue(result.get(1).getActive());
        // Booleanセルからの変換（false）
        assertFalse(result.get(2).getActive());
    }

    @Test
    @DisplayName("存在しないシート名 - SheetNotFoundExceptionを投げること")
    void testInvalidSheetName() {
        SheetNotFoundException exception = assertThrows(SheetNotFoundException.class, () -> {
            ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL)
                .sheetName("存在しないシート")
                .extract(stream -> stream.collect(Collectors.toList()));
        });

        assertTrue(exception.getMessage().contains("存在しないシート"));
    }

    private static void createSampleExcelWithDate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_WITH_DATE.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");
            CreationHelper createHelper = workbook.getCreationHelper();

            // 日付用のセルスタイルを作成
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));

            // 日時用のセルスタイルを作成
            CellStyle dateTimeStyle = workbook.createCellStyle();
            dateTimeStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));

            // ヘッダー行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("誕生日");
            headerRow.createCell(2).setCellValue("登録日時");

            // データ行1（日付セルを使用）
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("田中太郎");
            
            // 誕生日: 1990-05-15
            Calendar birthDate1 = Calendar.getInstance();
            birthDate1.set(1990, Calendar.MAY, 15, 0, 0, 0);
            birthDate1.set(Calendar.MILLISECOND, 0);
            Cell dateCell1 = dataRow1.createCell(1);
            dateCell1.setCellValue(birthDate1);
            dateCell1.setCellStyle(dateStyle);
            
            // 登録日時: 2024-01-15 10:30:00
            Calendar registeredAt1 = Calendar.getInstance();
            registeredAt1.set(2024, Calendar.JANUARY, 15, 10, 30, 0);
            registeredAt1.set(Calendar.MILLISECOND, 0);
            Cell dateTimeCell1 = dataRow1.createCell(2);
            dateTimeCell1.setCellValue(registeredAt1);
            dateTimeCell1.setCellStyle(dateTimeStyle);

            // データ行2（日付セルを使用）
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("佐藤花子");
            
            // 誕生日: 1985-12-25
            Calendar birthDate2 = Calendar.getInstance();
            birthDate2.set(1985, Calendar.DECEMBER, 25, 0, 0, 0);
            birthDate2.set(Calendar.MILLISECOND, 0);
            Cell dateCell2 = dataRow2.createCell(1);
            dateCell2.setCellValue(birthDate2);
            dateCell2.setCellStyle(dateStyle);
            
            // 登録日時: 2024-02-20 15:45:30
            Calendar registeredAt2 = Calendar.getInstance();
            registeredAt2.set(2024, Calendar.FEBRUARY, 20, 15, 45, 30);
            registeredAt2.set(Calendar.MILLISECOND, 0);
            Cell dateTimeCell2 = dataRow2.createCell(2);
            dateTimeCell2.setCellValue(registeredAt2);
            dateTimeCell2.setCellStyle(dateTimeStyle);

            workbook.write(fos);
        }
    }

    @Test
    @DisplayName("LocalDate型変換 - 日付セルからLocalDateを正しく読み込めること")
    void testLocalDateConversion() throws IOException {
        List<PersonWithDate> result = ExcelStreamReader.builder(PersonWithDate.class, SAMPLE_EXCEL_WITH_DATE)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(2, result.size());

        // 1990-05-15
        assertEquals(LocalDate.of(1990, 5, 15), result.get(0).getBirthDate());
        // 1985-12-25
        assertEquals(LocalDate.of(1985, 12, 25), result.get(1).getBirthDate());
    }

    @Test
    @DisplayName("LocalDateTime型変換 - 日時セルからLocalDateTimeを正しく読み込めること")
    void testLocalDateTimeConversion() throws IOException {
        List<PersonWithDate> result = ExcelStreamReader.builder(PersonWithDate.class, SAMPLE_EXCEL_WITH_DATE)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(2, result.size());

        // 2024-01-15T10:30:00
        LocalDateTime expected1 = LocalDateTime.of(2024, 1, 15, 10, 30, 0);
        assertEquals(expected1, result.get(0).getRegisteredAt());
        
        // 2024-02-20T15:45:30
        LocalDateTime expected2 = LocalDateTime.of(2024, 2, 20, 15, 45, 30);
        assertEquals(expected2, result.get(1).getRegisteredAt());
    }

    private static void createSampleExcelWithFormula() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_WITH_FORMULA.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行1（通常のセル）
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("田中太郎");
            dataRow1.createCell(1).setCellValue(25);
            dataRow1.createCell(2).setCellValue("エンジニア");
            dataRow1.createCell(3).setCellValue("東京");

            // データ行2（数式セルを含む）
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("佐藤花子");
            // 年齢を数式で設定（25 + 5 = 30）
            Cell ageCell = dataRow2.createCell(1);
            ageCell.setCellFormula("25+5");
            dataRow2.createCell(2).setCellValue("デザイナー");
            dataRow2.createCell(3).setCellValue("大阪");

            workbook.write(fos);
        }
    }

    /**
     * カスタムコンバーター（@CsvCustomBindByName）検証用Excel（ヘッダーあり）。
     */
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

    /**
     * カスタムコンバーター（@CsvCustomBindByPosition）検証用Excel（ヘッダーなし）。
     */
    private static void createSampleExcelWithCustomConverterPosition() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_CUSTOM_CONVERTER_POSITION.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダーなし、[名前, 年齢, 職業] の順で配置
            Row row1 = sheet.createRow(0);
            row1.createCell(0).setCellValue("田中太郎");
            row1.createCell(1).setCellValue(" 25 ");
            row1.createCell(2).setCellValue(" engineer ");

            Row row2 = sheet.createRow(1);
            row2.createCell(0).setCellValue("佐藤花子");
            row2.createCell(1).setCellValue("30");
            row2.createCell(2).setCellValue("designer");

            workbook.write(fos);
        }
    }

    @Test
    @DisplayName("数式セル - 数式セルを含むExcelを読み込むとNumberFormatExceptionが発生すること")
    void testFormulaCell() {
        // 数式セルは文字列として扱われるため、Integer変換で例外が発生する
        assertThrows(Exception.class, () -> {
            ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_WITH_FORMULA)
                .extract(stream -> stream.collect(Collectors.toList()));
        });
    }

    @Test
    @DisplayName("空のシート - 空のシートを読み込んでも例外が発生しないこと")
    void testEmptySheet() throws IOException {
        // 空のExcelファイルを作成
        Path emptyExcel = TEST_RESOURCES_DIR.resolve("empty.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(emptyExcel.toFile())) {
            workbook.createSheet("Sheet1");
            workbook.write(fos);
        }

        List<Person> result = ExcelStreamReader.builder(Person.class, emptyExcel)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("ファイル存在チェック - 存在しないファイルを読み込もうとすると例外が発生すること")
    void testFileNotFound() {
        Path nonExistentFile = TEST_RESOURCES_DIR.resolve("non_existent.xlsx");
        
        assertThrows(Exception.class, () -> {
            ExcelStreamReader.builder(Person.class, nonExistentFile)
                .extract(stream -> stream.collect(Collectors.toList()));
        });
    }

    @Test
    @DisplayName("複雑なセル型 - 空白セル、BLANK型を含むExcelを読み込めること")
    void testComplexCellTypes() throws IOException {
        // 様々なセル型を含むExcelファイルを作成
        Path complexExcel = TEST_RESOURCES_DIR.resolve("complex_cells.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(complexExcel.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行1（一部のセルが空白）
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("田中太郎");
            dataRow1.createCell(1).setCellValue(25);
            // セル2はnull（作成しない）
            dataRow1.createCell(3).setCellValue("東京");

            // データ行2（全てのセルに値がある）
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("佐藤花子");
            dataRow2.createCell(1).setCellValue(30);
            dataRow2.createCell(2).setCellValue("デザイナー");
            dataRow2.createCell(3).setCellValue("大阪");

            // データ行3（明示的にBLANK型のセルを作成）
            Row dataRow3 = sheet.createRow(3);
            dataRow3.createCell(0).setCellValue("山田次郎");
            dataRow3.createCell(1).setCellValue(28);
            Cell blankCell = dataRow3.createCell(2);
            blankCell.setBlank();
            dataRow3.createCell(3).setCellValue("福岡");

            workbook.write(fos);
        }

        List<Person> result = ExcelStreamReader.builder(Person.class, complexExcel)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size());

        // 1行目: 職業がnull
        assertEquals("田中太郎", result.get(0).getName());
        assertNull(result.get(0).getOccupation());

        // 2行目: 全てのフィールドに値がある
        assertEquals("佐藤花子", result.get(1).getName());
        assertEquals("デザイナー", result.get(1).getOccupation());

        // 3行目: 職業がBLANK型
        assertEquals("山田次郎", result.get(2).getName());
        assertTrue(result.get(2).getOccupation() == null || result.get(2).getOccupation().isEmpty());
    }

    @Test
    @DisplayName("全行が空 - 全ての行が空の場合は空のリストが返されること")
    void testAllEmptyRows() throws IOException {
        Path allEmptyExcel = TEST_RESOURCES_DIR.resolve("all_empty.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(allEmptyExcel.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // 空行を3つ作成
            sheet.createRow(1);
            sheet.createRow(2);
            sheet.createRow(3);

            workbook.write(fos);
        }

        List<Person> result = ExcelStreamReader.builder(Person.class, allEmptyExcel)
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("カスタムコンバーター - @CsvCustomBindByName が ExcelStreamReader でも適用されること")
    void testCustomConverterWithHeaderMapping() throws IOException {
        List<PersonWithCustomConverter> result = ExcelStreamReader
            .builder(PersonWithCustomConverter.class, SAMPLE_EXCEL_CUSTOM_CONVERTER)
            .useHeaderMapping()
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(2, result.size());

        PersonWithCustomConverter p1 = result.get(0);
        assertEquals("田中太郎", p1.getName());
        // " 25 " -> 25 に変換されていること
        assertEquals(25, p1.getAge());
        // occupation は @CsvCustomBindByPosition なので、このテストでは null のまま
        assertNull(p1.getOccupation());

        PersonWithCustomConverter p2 = result.get(1);
        assertEquals("佐藤花子", p2.getName());
        assertEquals(30, p2.getAge());
        assertNull(p2.getOccupation());
    }

    @Test
    @DisplayName("カスタムコンバーター - @CsvCustomBindByPosition が ExcelStreamReader の位置マッピングでも適用されること")
    void testCustomConverterWithPositionMapping() throws IOException {
        List<PersonWithCustomConverter> result = ExcelStreamReader
            .builder(PersonWithCustomConverter.class, SAMPLE_EXCEL_CUSTOM_CONVERTER_POSITION)
            .usePositionMapping()
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(2, result.size());

        PersonWithCustomConverter p1 = result.get(0);
        assertEquals("田中太郎", p1.getName());
        // age は @CsvCustomBindByName なので、このテストでは null のまま
        assertNull(p1.getAge());
        // " engineer " -> "ENGINEER" に変換されていること
        assertEquals("ENGINEER", p1.getOccupation());

        PersonWithCustomConverter p2 = result.get(1);
        assertEquals("佐藤花子", p2.getName());
        assertNull(p2.getAge());
        assertEquals("DESIGNER", p2.getOccupation());
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

        List<Person> result = ExcelStreamReader.builder(Person.class, missingCellsExcel)
            .extract(stream -> stream.collect(Collectors.toList()));

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
        List<Person> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_FULLWIDTH)
            .extract(stream -> stream.collect(Collectors.toList()));

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
