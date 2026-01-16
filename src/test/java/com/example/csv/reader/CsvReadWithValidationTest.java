package com.example.csv.reader;

import com.example.model.Person;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import com.opencsv.exceptions.CsvException;

@DisplayName("CsvReaderWrapper: 列数チェック機能（readWithValidation）")
public class CsvReadWithValidationTest {

    private static final Path TEST_RESOURCES_DIR = Paths.get("src/test/resources");
    private static final Path SAMPLE_CSV_COLUMN_MISMATCH = TEST_RESOURCES_DIR.resolve("sample_column_mismatch.csv");

    @BeforeAll
    static void setUp() throws IOException {
        // テストリソースディレクトリを作成
        Files.createDirectories(TEST_RESOURCES_DIR);

        // 列数不一致のサンプルCSVファイルを作成
        createSampleCsvWithColumnMismatch();
    }

    private static void createSampleCsvWithColumnMismatch() throws IOException {
        // ヘッダー行（4列）
        // データ行1（正常：4列）
        // データ行2（列数不一致：5列）
        // データ行3（正常：4列）
        // データ行4（列数不一致：3列）
        // データ行5（正常：4列）
        String content = "名前,年齢,職業,出身地\n"
            + "田中太郎,25,エンジニア,東京\n"
            + "佐藤花子,30,デザイナー,大阪,余分な列\n"
            + "山田次郎,28,営業,福岡\n"
            + "高橋健太,35,マネージャー\n"
            + "伊藤美咲,27,エンジニア,札幌\n";

        Files.write(SAMPLE_CSV_COLUMN_MISMATCH, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    @Test
    @DisplayName("列数不一致の行をスキップして最後まで読み込めること")
    void testReadWithValidation() throws IOException, CsvException {
        CsvReadResult<Person> result = CsvReaderWrapper.builder(Person.class, SAMPLE_CSV_COLUMN_MISMATCH)
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
        List<CsvReadError> errors = result.getErrors();
        assertEquals(2, errors.size());
        
        // 行3（5列）のエラー
        CsvReadError error1 = errors.get(0);
        assertEquals(3, error1.getLineNumber());
        assertEquals(4, error1.getExpectedColumnCount());
        assertEquals(5, error1.getActualColumnCount());
        assertTrue(error1.getMessage().contains("列数が不一致です"));

        // 行5（3列）のエラー
        CsvReadError error2 = errors.get(1);
        assertEquals(5, error2.getLineNumber());
        assertEquals(4, error2.getExpectedColumnCount());
        assertEquals(3, error2.getActualColumnCount());
        assertTrue(error2.getMessage().contains("列数が不一致です"));
    }
    
    @Test
    @DisplayName("列数不一致の行をスキップした場合でも、行番号は元のファイルの行番号が設定されること")
    void testReadWithValidationLineNumbers() throws IOException, CsvException {
        // 行番号フィールドを持つBeanクラスを使用
        CsvReadResult<com.example.model.linenumber.PersonWithLineNumber> result = 
            CsvReaderWrapper.builder(com.example.model.linenumber.PersonWithLineNumber.class, SAMPLE_CSV_COLUMN_MISMATCH)
                .readWithValidation();

        assertNotNull(result);
        assertEquals(3, result.getSuccessCount());
        
        List<com.example.model.linenumber.PersonWithLineNumber> data = result.getData();
        
        // 1つ目のBeanは元のファイルの2行目（ヘッダー行の次）
        assertEquals(2, data.get(0).getLineNumber());
        assertEquals("田中太郎", data.get(0).getName());
        
        // 2つ目のBeanは元のファイルの4行目（3行目がエラー行なのでスキップ）
        assertEquals(4, data.get(1).getLineNumber());
        assertEquals("山田次郎", data.get(1).getName());
        
        // 3つ目のBeanは元のファイルの6行目（5行目がエラー行なのでスキップ）
        assertEquals(6, data.get(2).getLineNumber());
        assertEquals("伊藤美咲", data.get(2).getName());
    }

    @Test
    @DisplayName("CsvStreamReaderでも列数不一致の行をスキップして最後まで読み込めること")
    void testStreamReaderReadWithValidation() throws IOException, CsvException {
        CsvReadResult<Person> result = CsvStreamReader.builder(Person.class, SAMPLE_CSV_COLUMN_MISMATCH)
            .readWithValidation();

        assertNotNull(result);
        assertEquals(3, result.getSuccessCount());
        assertEquals(2, result.getErrorCount());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("列数が全て一致している場合はエラーが0件であること")
    void testReadWithValidationNoErrors() throws IOException, CsvException {
        // 正常なファイルを使用
        CsvReadResult<Person> result = CsvReaderWrapper.builder(Person.class, 
            Paths.get("src/test/resources/sample.csv"))
            .readWithValidation();

        assertNotNull(result);
        assertFalse(result.hasErrors());
        assertEquals(0, result.getErrorCount());
        assertTrue(result.getSuccessCount() > 0);
    }

    @Test
    @DisplayName("TSVファイルでも列数不一致の行をスキップできること")
    void testReadWithValidationTsv() throws IOException, CsvException {
        // TSVファイルを作成
        Path tsvFile = TEST_RESOURCES_DIR.resolve("sample_column_mismatch.tsv");
        String content = "名前\t年齢\t職業\t出身地\n"
            + "田中太郎\t25\tエンジニア\t東京\n"
            + "佐藤花子\t30\tデザイナー\t大阪\t余分な列\n"
            + "山田次郎\t28\t営業\t福岡\n";

        Files.write(tsvFile, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        CsvReadResult<Person> result = CsvReaderWrapper.builder(Person.class, tsvFile)
            .fileType(com.example.common.config.FileType.TSV)
            .readWithValidation();

        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getErrorCount());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("空行が含まれるファイルで列数不一致の行をスキップできること")
    void testReadWithValidationWithEmptyLines() throws IOException, CsvException {
        // 空行が含まれるCSVファイルを作成
        Path csvFileWithEmptyLines = TEST_RESOURCES_DIR.resolve("sample_with_empty_lines.csv");
        String content = "名前,年齢,職業,出身地\n"
            + "\n"  // 空行1
            + "田中太郎,25,エンジニア,東京\n"
            + "佐藤花子,30,デザイナー,大阪,余分な列\n"  // エラー行（5列）
            + "\n"  // 空行2
            + "山田次郎,28,営業,福岡\n"
            + "高橋健太,35,マネージャー\n"  // エラー行（3列）
            + "\n"  // 空行3
            + "伊藤美咲,27,エンジニア,札幌\n";

        Files.write(csvFileWithEmptyLines, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        CsvReadResult<Person> result = CsvReaderWrapper.builder(Person.class, csvFileWithEmptyLines)
            .readWithValidation();

        assertNotNull(result);
        // 成功した行は3件（田中太郎、山田次郎、伊藤美咲）
        assertEquals(3, result.getSuccessCount());
        // エラー行は2件（佐藤花子、高橋健太）
        assertEquals(2, result.getErrorCount());
        assertTrue(result.hasErrors());

        // 成功したデータの確認
        List<Person> data = result.getData();
        assertEquals(3, data.size());
        assertEquals("田中太郎", data.get(0).getName());
        assertEquals("山田次郎", data.get(1).getName());
        assertEquals("伊藤美咲", data.get(2).getName());

        // エラー行の確認（論理的行番号でチェック）
        List<CsvReadError> errors = result.getErrors();
        assertEquals(2, errors.size());
        
        // エラー行1: 佐藤花子（論理的行番号3、空行をスキップ）
        CsvReadError error1 = errors.get(0);
        assertEquals(3, error1.getLineNumber());
        assertEquals(4, error1.getExpectedColumnCount());
        assertEquals(5, error1.getActualColumnCount());
        
        // エラー行2: 高橋健太（論理的行番号5、空行をスキップ）
        CsvReadError error2 = errors.get(1);
        assertEquals(5, error2.getLineNumber());
        assertEquals(4, error2.getExpectedColumnCount());
        assertEquals(3, error2.getActualColumnCount());
    }

    @Test
    @DisplayName("空行が含まれるファイルで行番号が正しくマッピングされること")
    void testReadWithValidationLineNumbersWithEmptyLines() throws IOException, CsvException {
        // 空行が含まれるCSVファイルを作成
        Path csvFileWithEmptyLines = TEST_RESOURCES_DIR.resolve("sample_with_empty_lines_linenumber.csv");
        String content = "名前,年齢,職業,出身地\n"
            + "\n"  // 空行1
            + "田中太郎,25,エンジニア,東京\n"
            + "佐藤花子,30,デザイナー,大阪,余分な列\n"  // エラー行（5列）
            + "\n"  // 空行2
            + "山田次郎,28,営業,福岡\n"
            + "高橋健太,35,マネージャー\n"  // エラー行（3列）
            + "\n"  // 空行3
            + "伊藤美咲,27,エンジニア,札幌\n";

        Files.write(csvFileWithEmptyLines, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        CsvReadResult<com.example.model.linenumber.PersonWithLineNumber> result = 
            CsvReaderWrapper.builder(com.example.model.linenumber.PersonWithLineNumber.class, csvFileWithEmptyLines)
                .readWithValidation();

        assertNotNull(result);
        assertEquals(3, result.getSuccessCount());
        
        List<com.example.model.linenumber.PersonWithLineNumber> data = result.getData();
        
        // 1つ目のBeanは元のファイルの3行目（ヘッダー行の次、空行をスキップした論理的行番号2）
        assertEquals(2, data.get(0).getLineNumber());
        assertEquals("田中太郎", data.get(0).getName());
        
        // 2つ目のBeanは元のファイルの6行目（4行目がエラー行なのでスキップ、空行をスキップした論理的行番号4）
        assertEquals(4, data.get(1).getLineNumber());
        assertEquals("山田次郎", data.get(1).getName());
        
        // 3つ目のBeanは元のファイルの8行目（6行目がエラー行なのでスキップ、空行をスキップした論理的行番号6）
        assertEquals(6, data.get(2).getLineNumber());
        assertEquals("伊藤美咲", data.get(2).getName());
    }

    @Test
    @DisplayName("位置ベース+スキップ行ありでもエラー行を除外し、行番号が元ファイル基準になること")
    void testReadWithValidationPositionMappingAndSkipLines() throws IOException, CsvException {
        Path csvFile = TEST_RESOURCES_DIR.resolve("sample_position_skip_with_errors.csv");
        String content = String.join(System.lineSeparator(),
            "田中太郎,20,営業",
            "佐藤花子,30,開発",
            "山田次郎,40",
            "高橋健太,50,総務"
        );
        Files.writeString(csvFile, content, StandardCharsets.UTF_8);

        CsvReadResult<com.example.model.linenumber.PersonWithoutHeaderAndLineNumber> result =
            CsvReaderWrapper.builder(com.example.model.linenumber.PersonWithoutHeaderAndLineNumber.class, csvFile)
                .usePositionMapping()
                .skipLines(1)
                .readWithValidation();

        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getErrorCount());

        List<com.example.model.linenumber.PersonWithoutHeaderAndLineNumber> data = result.getData();
        assertEquals("佐藤花子", data.get(0).getName());
        assertEquals(2, data.get(0).getLineNumber());
        assertEquals("高橋健太", data.get(1).getName());
        assertEquals(4, data.get(1).getLineNumber());
    }

    @Test
    @DisplayName("CsvStreamReaderで空行が含まれるファイルでも列数不一致の行をスキップできること")
    void testStreamReaderReadWithValidationWithEmptyLines() throws IOException, CsvException {
        // 空行が含まれるCSVファイルを作成
        Path csvFileWithEmptyLines = TEST_RESOURCES_DIR.resolve("sample_with_empty_lines_stream.csv");
        String content = "名前,年齢,職業,出身地\n"
            + "\n"  // 空行1
            + "田中太郎,25,エンジニア,東京\n"
            + "佐藤花子,30,デザイナー,大阪,余分な列\n"  // エラー行（5列）
            + "\n"  // 空行2
            + "山田次郎,28,営業,福岡\n"
            + "高橋健太,35,マネージャー\n"  // エラー行（3列）
            + "\n"  // 空行3
            + "伊藤美咲,27,エンジニア,札幌\n";

        Files.write(csvFileWithEmptyLines, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        CsvReadResult<Person> result = CsvStreamReader.builder(Person.class, csvFileWithEmptyLines)
            .readWithValidation();

        assertNotNull(result);
        assertEquals(3, result.getSuccessCount());
        assertEquals(2, result.getErrorCount());
        assertTrue(result.hasErrors());

        // 成功したデータの確認
        List<Person> data = result.getData();
        assertEquals(3, data.size());
        assertEquals("田中太郎", data.get(0).getName());
        assertEquals("山田次郎", data.get(1).getName());
        assertEquals("伊藤美咲", data.get(2).getName());
    }

    @Test
    @DisplayName("CsvStreamReaderで空行が含まれるファイルでも行番号が正しくマッピングされること")
    void testStreamReaderReadWithValidationLineNumbersWithEmptyLines() throws IOException, CsvException {
        // 空行が含まれるCSVファイルを作成
        Path csvFileWithEmptyLines = TEST_RESOURCES_DIR.resolve("sample_with_empty_lines_stream_linenumber.csv");
        String content = "名前,年齢,職業,出身地\n"
            + "\n"  // 空行1
            + "田中太郎,25,エンジニア,東京\n"
            + "佐藤花子,30,デザイナー,大阪,余分な列\n"  // エラー行（5列）
            + "\n"  // 空行2
            + "山田次郎,28,営業,福岡\n"
            + "高橋健太,35,マネージャー\n"  // エラー行（3列）
            + "\n"  // 空行3
            + "伊藤美咲,27,エンジニア,札幌\n";

        Files.write(csvFileWithEmptyLines, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        CsvReadResult<com.example.model.linenumber.PersonWithLineNumber> result = 
            CsvStreamReader.builder(com.example.model.linenumber.PersonWithLineNumber.class, csvFileWithEmptyLines)
                .readWithValidation();

        assertNotNull(result);
        assertEquals(3, result.getSuccessCount());
        
        List<com.example.model.linenumber.PersonWithLineNumber> data = result.getData();
        
        // 1つ目のBeanは元のファイルの3行目（ヘッダー行の次、空行をスキップした論理的行番号2）
        assertEquals(2, data.get(0).getLineNumber());
        assertEquals("田中太郎", data.get(0).getName());
        
        // 2つ目のBeanは元のファイルの6行目（4行目がエラー行なのでスキップ、空行をスキップした論理的行番号4）
        assertEquals(4, data.get(1).getLineNumber());
        assertEquals("山田次郎", data.get(1).getName());
        
        // 3つ目のBeanは元のファイルの8行目（6行目がエラー行なのでスキップ、空行をスキップした論理的行番号6）
        assertEquals(6, data.get(2).getLineNumber());
        assertEquals("伊藤美咲", data.get(2).getName());
    }

    @Test
    @DisplayName("引用符で囲まれたフィールド内に改行が含まれるCSVでも正しく処理できること")
    void testReadWithValidationWithEmbeddedNewlines() throws IOException, CsvException {
        // 引用符で囲まれたフィールド内に改行が含まれるCSVファイルを作成
        Path csvFileWithNewlines = TEST_RESOURCES_DIR.resolve("sample_with_embedded_newlines.csv");
        String content = "名前,年齢,職業,出身地\n"
            + "\"田中\n太郎\",25,エンジニア,東京\n"  // 名前フィールドに改行が含まれる
            + "\"佐藤\n花子\",30,デザイナー,大阪,余分な列\n"  // エラー行（5列）+ 名前フィールドに改行
            + "\"山田\n次郎\",28,営業,福岡\n";  // 名前フィールドに改行が含まれる

        Files.write(csvFileWithNewlines, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        CsvReadResult<Person> result = CsvReaderWrapper.builder(Person.class, csvFileWithNewlines)
            .readWithValidation();

        assertNotNull(result);
        // 成功した行は2件（田中太郎、山田次郎）
        assertEquals(2, result.getSuccessCount());
        // エラー行は1件（佐藤花子）
        assertEquals(1, result.getErrorCount());
        assertTrue(result.hasErrors());

        // 成功したデータの確認
        List<Person> data = result.getData();
        assertEquals(2, data.size());
        // 改行が含まれるフィールドも正しく読み込まれていることを確認
        assertEquals("田中\n太郎", data.get(0).getName());
        assertEquals("山田\n次郎", data.get(1).getName());

        // エラー行の確認
        List<CsvReadError> errors = result.getErrors();
        assertEquals(1, errors.size());
        CsvReadError error = errors.get(0);
        assertEquals(3, error.getLineNumber()); // 論理的行番号3
        assertEquals(4, error.getExpectedColumnCount());
        assertEquals(5, error.getActualColumnCount());
    }

    @Test
    @DisplayName("CsvStreamReaderで引用符で囲まれたフィールド内に改行が含まれるCSVでも正しく処理できること")
    void testStreamReaderReadWithValidationWithEmbeddedNewlines() throws IOException, CsvException {
        // 引用符で囲まれたフィールド内に改行が含まれるCSVファイルを作成
        Path csvFileWithNewlines = TEST_RESOURCES_DIR.resolve("sample_with_embedded_newlines_stream.csv");
        String content = "名前,年齢,職業,出身地\n"
            + "\"田中\n太郎\",25,エンジニア,東京\n"  // 名前フィールドに改行が含まれる
            + "\"佐藤\n花子\",30,デザイナー,大阪,余分な列\n"  // エラー行（5列）+ 名前フィールドに改行
            + "\"山田\n次郎\",28,営業,福岡\n";  // 名前フィールドに改行が含まれる

        Files.write(csvFileWithNewlines, content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        CsvReadResult<Person> result = CsvStreamReader.builder(Person.class, csvFileWithNewlines)
            .readWithValidation();

        assertNotNull(result);
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getErrorCount());
        assertTrue(result.hasErrors());

        // 成功したデータの確認
        List<Person> data = result.getData();
        assertEquals(2, data.size());
        // 改行が含まれるフィールドも正しく読み込まれていることを確認
        assertEquals("田中\n太郎", data.get(0).getName());
        assertEquals("山田\n次郎", data.get(1).getName());
    }
}
