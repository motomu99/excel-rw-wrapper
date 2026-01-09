package com.example.excel.reader;

import com.example.model.Person;
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

@DisplayName("ExcelReader: 列数チェック機能（readWithValidation）")
public class ExcelReadWithValidationTest {

    private static final Path TEST_RESOURCES_DIR = Paths.get("src/test/resources");
    private static final Path SAMPLE_EXCEL_COLUMN_MISMATCH = TEST_RESOURCES_DIR.resolve("sample_column_mismatch.xlsx");

    @BeforeAll
    static void setUp() throws IOException {
        // テストリソースディレクトリを作成
        Files.createDirectories(TEST_RESOURCES_DIR);

        // 列数不一致のサンプルExcelファイルを作成
        createSampleExcelWithColumnMismatch();
    }

    private static void createSampleExcelWithColumnMismatch() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(SAMPLE_EXCEL_COLUMN_MISMATCH.toFile())) {

            Sheet sheet = workbook.createSheet("Sheet1");

            // ヘッダー行（4列）
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行1（正常：4列）
            Row dataRow1 = sheet.createRow(1);
            dataRow1.createCell(0).setCellValue("田中太郎");
            dataRow1.createCell(1).setCellValue(25);
            dataRow1.createCell(2).setCellValue("エンジニア");
            dataRow1.createCell(3).setCellValue("東京");

            // データ行2（列数不一致：5列）
            Row dataRow2 = sheet.createRow(2);
            dataRow2.createCell(0).setCellValue("佐藤花子");
            dataRow2.createCell(1).setCellValue(30);
            dataRow2.createCell(2).setCellValue("デザイナー");
            dataRow2.createCell(3).setCellValue("大阪");
            dataRow2.createCell(4).setCellValue("余分な列");

            // データ行3（正常：4列）
            Row dataRow3 = sheet.createRow(3);
            dataRow3.createCell(0).setCellValue("山田次郎");
            dataRow3.createCell(1).setCellValue(28);
            dataRow3.createCell(2).setCellValue("営業");
            dataRow3.createCell(3).setCellValue("福岡");

            // データ行4（列数不一致：3列）
            Row dataRow4 = sheet.createRow(4);
            dataRow4.createCell(0).setCellValue("高橋健太");
            dataRow4.createCell(1).setCellValue(35);
            dataRow4.createCell(2).setCellValue("マネージャー");

            // データ行5（正常：4列）
            Row dataRow5 = sheet.createRow(5);
            dataRow5.createCell(0).setCellValue("伊藤美咲");
            dataRow5.createCell(1).setCellValue(27);
            dataRow5.createCell(2).setCellValue("エンジニア");
            dataRow5.createCell(3).setCellValue("札幌");

            workbook.write(fos);
        }
    }

    @Test
    @DisplayName("列数不一致の行をスキップして最後まで読み込めること")
    void testReadWithValidation() throws IOException {
        ExcelReadResult<Person> result = ExcelReader.builder(Person.class, SAMPLE_EXCEL_COLUMN_MISMATCH)
            .readWithValidation();

        assertNotNull(result);
        
        // 成功した行は3件（行2, 4, 6）
        assertEquals(3, result.getSuccessCount());
        assertEquals(2, result.getErrorCount());
        assertTrue(result.hasErrors());

        // 成功したデータの確認
        List<Person> data = result.getData();
        assertEquals(3, data.size());
        assertEquals("田中太郎", data.get(0).getName());
        assertEquals("山田次郎", data.get(1).getName());
        assertEquals("伊藤美咲", data.get(2).getName());

        // エラー行の確認
        List<ExcelReadError> errors = result.getErrors();
        assertEquals(2, errors.size());
        
        // 行3（5列）のエラー
        ExcelReadError error1 = errors.get(0);
        assertEquals(3, error1.getLineNumber());
        assertEquals(4, error1.getExpectedColumnCount());
        assertEquals(5, error1.getActualColumnCount());
        assertTrue(error1.getMessage().contains("列数が不一致です"));

        // 行5（3列）のエラー
        ExcelReadError error2 = errors.get(1);
        assertEquals(5, error2.getLineNumber());
        assertEquals(4, error2.getExpectedColumnCount());
        assertEquals(3, error2.getActualColumnCount());
        assertTrue(error2.getMessage().contains("列数が不一致です"));
    }

    @Test
    @DisplayName("ExcelStreamReaderでも列数不一致の行をスキップして最後まで読み込めること")
    void testStreamReaderReadWithValidation() throws IOException {
        ExcelReadResult<Person> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_COLUMN_MISMATCH)
            .readWithValidation();

        assertNotNull(result);
        assertEquals(3, result.getSuccessCount());
        assertEquals(2, result.getErrorCount());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("列数が全て一致している場合はエラーが0件であること")
    void testReadWithValidationNoErrors() throws IOException {
        // 正常なファイルを使用
        ExcelReadResult<Person> result = ExcelReader.builder(Person.class, 
            Paths.get("src/test/resources/sample.xlsx"))
            .readWithValidation();

        assertNotNull(result);
        assertFalse(result.hasErrors());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.getSuccessCount() > 0);
    }
}
