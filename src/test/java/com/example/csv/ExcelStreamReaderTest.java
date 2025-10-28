package com.example.csv;

import com.example.csv.model.Person;
import com.example.csv.model.PersonWithoutHeader;

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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@DisplayName("ExcelStreamReader: Stream APIを使用したExcel読み込み")
public class ExcelStreamReaderTest {

    private static final Path TEST_RESOURCES_DIR = Paths.get("src/test/resources");
    private static final Path SAMPLE_EXCEL = TEST_RESOURCES_DIR.resolve("sample.xlsx");
    private static final Path SAMPLE_EXCEL_NO_HEADER = TEST_RESOURCES_DIR.resolve("sample_no_header.xlsx");
    private static final Path SAMPLE_EXCEL_MULTI_SHEET = TEST_RESOURCES_DIR.resolve("sample_multi_sheet.xlsx");

    @BeforeAll
    static void setUp() throws IOException {
        // テストリソースディレクトリを作成
        Files.createDirectories(TEST_RESOURCES_DIR);

        // 基本的なサンプルExcelファイルを作成
        createSampleExcel();

        // ヘッダーなしのサンプルExcelファイルを作成
        createSampleExcelNoHeader();

        // 複数シートのサンプルExcelファイルを作成
        createSampleExcelMultiSheet();
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

            // ヘッダー行（位置マッピング用のダミー）
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("col0");
            headerRow.createCell(1).setCellValue("col1");

            // データ行を作成（名前と年齢のみ）
            createSimpleDataRow(sheet, 1, "田中太郎", 25);
            createSimpleDataRow(sheet, 2, "佐藤花子", 30);
            createSimpleDataRow(sheet, 3, "山田次郎", 28);
            createSimpleDataRow(sheet, 4, "高橋健太", 35);
            createSimpleDataRow(sheet, 5, "伊藤美咲", 27);

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

    @Test
    @DisplayName("基本的なStream処理 - ExcelファイルをStreamとして読み込み、Listに変換できること")
    void testBasicStreamProcessing() throws IOException {
        List<Person> result = ExcelStreamReader.of(Person.class, SAMPLE_EXCEL)
            .process(stream -> stream.collect(Collectors.toList()));

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
        List<Person> result = ExcelStreamReader.of(Person.class, SAMPLE_EXCEL)
            .process(stream -> stream
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
        List<String> names = ExcelStreamReader.of(Person.class, SAMPLE_EXCEL)
            .process(stream -> stream
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
        List<Person> result = ExcelStreamReader.of(Person.class, SAMPLE_EXCEL)
            .skip(2)
            .process(stream -> stream.collect(Collectors.toList()));

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
        List<Person> result1 = ExcelStreamReader.of(Person.class, SAMPLE_EXCEL_MULTI_SHEET)
            .sheetIndex(0)
            .process(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result1);
        assertEquals(2, result1.size());
        assertEquals("田中太郎", result1.get(0).getName());

        // Sheet2（インデックス1）を読み込む
        List<Person> result2 = ExcelStreamReader.of(Person.class, SAMPLE_EXCEL_MULTI_SHEET)
            .sheetIndex(1)
            .process(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result2);
        assertEquals(3, result2.size());
        assertEquals("山田次郎", result2.get(0).getName());
    }

    @Test
    @DisplayName("シート名指定 - sheetName()メソッドで特定のシートを読み込めること")
    void testStreamWithSheetName() throws IOException {
        List<Person> result = ExcelStreamReader.of(Person.class, SAMPLE_EXCEL_MULTI_SHEET)
            .sheetName("データ2")
            .process(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("山田次郎", result.get(0).getName());
    }

    @Test
    @DisplayName("位置ベースマッピング - usePositionMapping()でヘッダーなしExcelを読み込めること")
    void testStreamWithPositionMapping() throws IOException {
        List<PersonWithoutHeader> result = ExcelStreamReader.of(PersonWithoutHeader.class, SAMPLE_EXCEL_NO_HEADER)
            .usePositionMapping()
            .process(stream -> stream.collect(Collectors.toList()));

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
        List<Person> result = ExcelStreamReader.of(Person.class, SAMPLE_EXCEL)
            .useHeaderMapping()
            .process(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(5, result.size());

        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
    }

    @Test
    @DisplayName("メソッドチェーン - 複数の設定とStream操作を組み合わせて使用できること")
    void testStreamWithChainedOperations() throws IOException {
        List<String> result = ExcelStreamReader.of(Person.class, SAMPLE_EXCEL)
            .skip(1)
            .sheetIndex(0)
            .useHeaderMapping()
            .process(stream -> stream
                .filter(person -> person.getAge() >= 25)
                .map(Person::getName)
                .collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size()); // 1行スキップして、25歳以上は3人

        assertTrue(result.contains("佐藤花子"));
        assertTrue(result.contains("山田次郎"));
        assertTrue(result.contains("高橋健太"));
    }

    @Test
    @DisplayName("カウント操作 - Stream.count()でレコード数を取得できること")
    void testStreamCount() throws IOException {
        Long count = ExcelStreamReader.of(Person.class, SAMPLE_EXCEL)
            .process(Stream::count);

        assertNotNull(count);
        assertEquals(5L, count);
    }

    @Test
    @DisplayName("forEach操作 - Stream.forEach()で各レコードを処理できること")
    void testStreamForEach() throws IOException {
        StringBuilder names = new StringBuilder();

        ExcelStreamReader.of(Person.class, SAMPLE_EXCEL)
            .process(stream -> {
                stream.forEach(person -> names.append(person.getName()).append(","));
                return null;
            });

        String result = names.toString();
        assertTrue(result.contains("田中太郎"));
        assertTrue(result.contains("佐藤花子"));
    }
}
