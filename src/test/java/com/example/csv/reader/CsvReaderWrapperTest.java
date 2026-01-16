package com.example.csv.reader;

import com.example.common.config.CharsetType;
import com.example.common.config.FileType;
import com.example.exception.CsvReadException;
import com.example.model.Person;
import com.example.model.PersonWithoutHeader;
import com.example.model.PersonWithCustomConverter;
import com.example.model.PersonWithCustomType;
import com.example.model.linenumber.PersonWithoutHeaderAndLineNumber;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * CsvReaderWrapperの新しいBuilderパターンAPIのテスト
 * 
 * <p>このクラスは推奨される新しいBuilderパターンのテストです。</p>
 * <p>レガシーAPIのテストは {@link CsvReaderWrapperLegacyTest} を参照してください。</p>
 */
@DisplayName("CsvReaderWrapper: 新しいBuilderパターンAPI")
public class CsvReaderWrapperTest {

    @Test
    @DisplayName("基本的な読み込み - デフォルト設定でCSVファイルを読み込めること")
    void testBuilderBasicUsage() {
        // Builderパターンの基本的な使い方
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .read();
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
    }

    @Test
    @DisplayName("データ行スキップ - 指定した行数をスキップして読み込めること")
    void testBuilderWithSkipLines() {
        // BuilderパターンでskipLines設定
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .skipLines(1)
            .read();
       
        assertNotNull(persons);
        assertEquals(4, persons.size());
        
        Person firstPerson = persons.get(0);
        assertEquals("佐藤花子", firstPerson.getName());
        assertEquals(30, firstPerson.getAge());
    }

    @Test
    @DisplayName("文字セット指定 - Shift_JISのCSVファイルを正しく読み込めること")
    void testBuilderWithCharset() {
        // Builderパターンでcharset設定
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample_sjis.csv"))
            .charset(CharsetType.S_JIS)
            .read();
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("ファイル形式指定 - TSVファイルを正しく読み込めること")
    void testBuilderWithFileType() {
        // BuilderパターンでfileType設定
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample.tsv"))
            .fileType(FileType.TSV)
            .read();
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("オプション指定 - ignoreQuotations(true)を指定しても通常のTSVを正しく読み込めること")
    void testBuilderWithIgnoreQuotationsOption() {
        // ignoreQuotations オプションを付与しても既存TSVが問題なく読めることを確認
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample.tsv"))
            .fileType(FileType.TSV)
            .ignoreQuotations(true)
            .read();

        assertNotNull(persons);
        assertEquals(5, persons.size());

        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("位置ベースマッピング - ヘッダーなしCSVを位置で読み込めること")
    void testBuilderWithPositionMapping() {
        // Builderパターンで位置ベースマッピング
        List<PersonWithoutHeader> persons = CsvReaderWrapper.builder(PersonWithoutHeader.class, Paths.get("src/test/resources/sample_no_header.csv"))
            .usePositionMapping()
            .read();
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        PersonWithoutHeader firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("BOM処理 - BOM付きUTF-8ファイルを正しく読み込めること")
    void testBuilderWithBom() {
        // BuilderパターンでBOM付きファイル
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample_utf8_bom.csv"))
            .charset(CharsetType.UTF_8_BOM)
            .read();
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("複数設定の組み合わせ - skipLines/fileType/headerMappingを同時指定できること")
    void testBuilderWithMultipleSettings() {
        // Builderパターンで複数設定の組み合わせ
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .skipLines(1)
            .fileType(FileType.CSV)
            .useHeaderMapping()
            .read();
       
        assertNotNull(persons);
        assertEquals(4, persons.size());
        
        Person firstPerson = persons.get(0);
        assertEquals("佐藤花子", firstPerson.getName());
        assertEquals(30, firstPerson.getAge());
    }

    @Test
    @DisplayName("境界値テスト - スキップ行数がデータ数以上の場合、空リストを返すこと")
    void testBuilderWithSkipLinesGreaterThanDataSize() {
        // Builderパターンでスキップ行数がデータ数以上
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .skipLines(10)
            .read();
       
        assertNotNull(persons);
        assertTrue(persons.isEmpty());
    }

    @Test
    @DisplayName("異常系 - 存在しないファイルの場合、CsvReadExceptionをスローすること")
    void testBuilderWithFileNotFound() {
        // Builderパターンで存在しないファイル
        assertThrows(CsvReadException.class, () -> {
            CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/nonexistent.csv"))
                .read();
        });
    }

    @Test
    @DisplayName("異常系 - 文字コード自動判別時にファイルが存在しない場合、CsvReadExceptionをスローすること")
    void testBuilderWithFileNotFoundDuringCharsetDetection() {
        // 文字コードを明示的に指定せず（自動判別）、存在しないファイルを読み込もうとした場合
        CsvReadException exception = assertThrows(CsvReadException.class, () -> {
            CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/nonexistent.csv"))
                // charsetTypeを指定しない = 自動判別が実行される
                .read();
        });
        
        // エラーメッセージにファイルパスが含まれていることを確認
        assertTrue(exception.getMessage().contains("nonexistent.csv"), 
            "エラーメッセージにファイルパスが含まれていること");
    }

    @Test
    @DisplayName("異常系 - CSV列数が不一致の場合にCsvReadExceptionをスローすること")
    void testBuilderWithColumnMismatchCsv() {
        assertThrows(CsvReadException.class, () -> {
            CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample_invalid_columns.csv"))
                .read();
        });
    }

    @Test
    @DisplayName("異常系 - TSV列数が不一致の場合にCsvReadExceptionをスローすること")
    void testBuilderWithColumnMismatchTsv() {
        assertThrows(CsvReadException.class, () -> {
            CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample_invalid_columns.tsv"))
                .fileType(FileType.TSV)
                .read();
        });
    }

    @Test
    @DisplayName("異常系 - 無効なファイルパスの場合、CsvReadExceptionをスローすること")
    void testBuilderWithInvalidPath() {
        // Builderパターンで無効なファイルパス
        assertThrows(CsvReadException.class, () -> {
            CsvReaderWrapper.builder(Person.class, Paths.get("invalid/path/file.csv"))
                .read();
        });
    }

    @Test
    @DisplayName("改行コード対応 - LF改行のみのファイルを正しく読み込めること")
    void testBuilderWithLfFile() {
        // BuilderパターンでLF改行ファイル
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample_lf.csv"))
            .read();
       
        assertNotNull(persons);
        assertEquals(3, persons.size());
        
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("カスタムコンバーター - ヘッダーマッピングで@CsvCustomBindByNameが適用されること")
    void testBuilderWithHeaderMappingAndCustomConverter() throws Exception {
        String csvContent = String.join(System.lineSeparator(),
            "名前,年齢,職業",
            "田中太郎, 40 ,エンジニア",
            "佐藤花子,30,デザイナー"
        );

        Path tempCsv = Files.createTempFile("csv-header-custom-converter-", ".csv");
        Files.writeString(tempCsv, csvContent, StandardCharsets.UTF_8);

        List<PersonWithCustomConverter> result = CsvReaderWrapper
            .builder(PersonWithCustomConverter.class, tempCsv)
            .useHeaderMapping()
            .read();

        assertNotNull(result);
        assertEquals(2, result.size());

        PersonWithCustomConverter first = result.get(0);
        assertEquals("田中太郎", first.getName());
        assertEquals(40, first.getAge());
        // occupation は @CsvCustomBindByPosition 用なので、このテストでは null のまま
        assertNull(first.getOccupation());

        PersonWithCustomConverter second = result.get(1);
        assertEquals("佐藤花子", second.getName());
        assertEquals(30, second.getAge());
        assertNull(second.getOccupation());
    }

    @Test
    @DisplayName("独自型とコンバーター - ポジションベースでヘッダーありCSVを読み込み、独自型に変換できること")
    void testBuilderWithCustomTypeAndConverter() {
        List<PersonWithCustomType> result = CsvReaderWrapper
            .builder(PersonWithCustomType.class, Paths.get("src/test/resources/sample.csv"))
            .usePositionMapping()
            .skipLines(1)
            .read();

        assertNotNull(result);
        assertEquals(5, result.size());

        PersonWithCustomType firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertNotNull(firstPerson.getEmail());
        assertEquals("エンジニア@example.com", firstPerson.getEmail().getValue());
        assertEquals("東京", firstPerson.getBirthplace());

        PersonWithCustomType secondPerson = result.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertNotNull(secondPerson.getEmail());
        assertEquals("デザイナー@example.com", secondPerson.getEmail().getValue());
        assertEquals("大阪", secondPerson.getBirthplace());

        assertTrue(result.stream().allMatch(person -> person.getEmail() != null),
            "すべてのレコードでEmailが設定されていること");
    }

    @Test
    @DisplayName("位置ベースのスキップ - スキップ後も行番号が正しく設定されること")
    void testBuilderWithPositionMappingAndSkipLinesLineNumber() throws Exception {
        String csvContent = String.join(System.lineSeparator(),
            "田中太郎,20,営業",
            "佐藤花子,30,開発",
            "山田次郎,40,営業"
        );

        Path tempCsv = Files.createTempFile("csv-position-skip-lines-", ".csv");
        Files.writeString(tempCsv, csvContent, StandardCharsets.UTF_8);

        List<PersonWithoutHeaderAndLineNumber> result = CsvReaderWrapper
            .builder(PersonWithoutHeaderAndLineNumber.class, tempCsv)
            .usePositionMapping()
            .skipLines(1)
            .read();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("佐藤花子", result.get(0).getName());
        assertEquals(2, result.get(0).getLineNumber());
        assertEquals("山田次郎", result.get(1).getName());
        assertEquals(3, result.get(1).getLineNumber());
    }

    @Test
    @DisplayName("ヘッダーマッピング - 明示的にヘッダーベースマッピングを指定できること")
    void testBuilderWithHeaderMapping() {
        // Builderパターンで明示的なヘッダーマッピング
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .useHeaderMapping()
            .read();
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("複雑なシナリオ - Shift_JIS/skipLines/headerMappingを組み合わせて読み込めること")
    void testBuilderComplexScenario() {
        // Builderパターンで複雑なシナリオ
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample_sjis.csv"))
            .charset(CharsetType.S_JIS)
            .skipLines(1)
            .useHeaderMapping()
            .read();
       
        assertNotNull(persons);
        assertEquals(4, persons.size());
        
        Person firstPerson = persons.get(0);
        assertEquals("佐藤花子", firstPerson.getName());
        assertEquals(30, firstPerson.getAge());
    }

    @Test
    @DisplayName("複数ファイル読み込み - 複数のCSVファイルを結合して読み込めること")
    void testBuilderWithMultipleFiles() {
        List<java.nio.file.Path> paths = java.util.Arrays.asList(
            Paths.get("src/test/resources/sample.csv"),
            Paths.get("src/test/resources/sample_lf.csv")
        );

        List<Person> persons = CsvReaderWrapper.builder(Person.class, paths)
            .readAll();

        assertNotNull(persons);
        assertEquals(5 + 3, persons.size()); // 5 + 3 = 8
        
        // 順序確認: sample.csv の最後 -> sample_lf.csv の最初
        assertEquals("田中太郎", persons.get(0).getName()); // sample.csv 1人目
        assertEquals("高橋健太", persons.get(4).getName()); // sample.csv 5人目
        assertEquals("田中太郎", persons.get(5).getName()); // sample_lf.csv 1人目
        assertEquals("鈴木一郎", persons.get(7).getName()); // sample_lf.csv 3人目
    }

    @Test
    @DisplayName("複数ファイル並列読み込み - 並列処理でも順序が維持されること")
    void testBuilderWithMultipleFilesParallel() {
        List<java.nio.file.Path> paths = java.util.Arrays.asList(
            Paths.get("src/test/resources/sample.csv"),
            Paths.get("src/test/resources/sample_lf.csv")
        );

        List<Person> persons = CsvReaderWrapper.builder(Person.class, paths)
            .parallelism(2)
            .readAll();

        assertNotNull(persons);
        assertEquals(8, persons.size());
        
        assertEquals("田中太郎", persons.get(0).getName());
        assertEquals("高橋健太", persons.get(4).getName());
        assertEquals("鈴木一郎", persons.get(7).getName());
    }
}
