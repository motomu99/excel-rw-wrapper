package com.example.csv;

import com.example.csv.model.Person;
import com.example.csv.model.Person2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import com.opencsv.exceptions.CsvException;

/**
 * CsvWriterWrapperの新しいBuilderパターンAPIのテスト
 * 
 * <p>このクラスは推奨される新しいBuilderパターンのテストです。</p>
 * <p>レガシーAPIのテストは {@link CsvWriterWrapperLegacyTest} を参照してください。</p>
 */
public class CsvWriterWrapperTest {

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
    void testBuilderBasicWrite() throws IOException, CsvException {
        // Builderパターンの基本的な書き込み
        Path outputPath = Paths.get("src/test/resources/builder_output_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("鈴木花子", 32, "デザイナー", "京都"));

        CsvWriterWrapper.builder(Person.class, outputPath)
            .write(persons);

        assertTrue(Files.exists(outputPath));

        // 読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .read();

        assertEquals(2, readPersons.size());
        assertEquals("山田太郎", readPersons.get(0).getName());
        assertEquals(28, readPersons.get(0).getAge());
    }

    @Test
    void testBuilderWithCharset() throws IOException, CsvException {
        // Builderパターンでcharset設定
        Path outputPath = Paths.get("src/test/resources/builder_sjis_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("テスト太郎", 25, "エンジニア", "東京"));

        CsvWriterWrapper.builder(Person.class, outputPath)
            .charset(CharsetType.S_JIS)
            .write(persons);

        assertTrue(Files.exists(outputPath));

        // Shift_JISで読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .charset(CharsetType.S_JIS)
            .read();

        assertEquals(1, readPersons.size());
        assertEquals("テスト太郎", readPersons.get(0).getName());
    }

    @Test
    void testBuilderWithFileType() throws IOException, CsvException {
        // BuilderパターンでTSV設定
        Path outputPath = Paths.get("src/test/resources/builder_test.tsv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("テスト太郎", 25, "エンジニア", "東京"));

        CsvWriterWrapper.builder(Person.class, outputPath)
            .fileType(FileType.TSV)
            .write(persons);

        assertTrue(Files.exists(outputPath));

        // TSVで読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .fileType(FileType.TSV)
            .read();

        assertEquals(1, readPersons.size());
        assertEquals("テスト太郎", readPersons.get(0).getName());
    }

    @Test
    void testBuilderWithLineSeparator() throws IOException {
        // Builderパターンで改行コード設定
        Path outputPath = Paths.get("src/test/resources/builder_lf_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("テスト太郎", 25, "エンジニア", "東京"));

        CsvWriterWrapper.builder(Person.class, outputPath)
            .lineSeparator(LineSeparatorType.LF)
            .write(persons);

        assertTrue(Files.exists(outputPath));

        // LFが含まれていることを確認（CRLFではない）
        byte[] fileBytes = Files.readAllBytes(outputPath);
        String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("\n"), "LFが含まれていません");
        assertFalse(content.contains("\r\n"), "CRLFが含まれています（LFのみであるべき）");
    }

    @Test
    void testBuilderWithPositionMapping() throws IOException, CsvException {
        // Builderパターンで位置ベースマッピング
        Path outputPath = Paths.get("src/test/resources/builder_position_test.csv");
        filesToDelete.add(outputPath);

        List<Person2> persons = new ArrayList<>();
        persons.add(new Person2("テスト太郎", 25, "エンジニア", "東京"));

        CsvWriterWrapper.builder(Person2.class, outputPath)
            .usePositionMapping()
            .write(persons);

        assertTrue(Files.exists(outputPath));

        // 位置ベースマッピングで読み込んで検証
        List<Person2> readPersons = CsvReaderWrapper.builder(Person2.class, outputPath)
            .usePositionMapping()
            .read();

        assertEquals(1, readPersons.size());
        assertEquals("テスト太郎", readPersons.get(0).getName());
    }

    @Test
    void testBuilderWithBom() throws IOException, CsvException {
        // BuilderパターンでBOM付き
        Path outputPath = Paths.get("src/test/resources/builder_bom_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("テスト太郎", 25, "エンジニア", "東京"));

        CsvWriterWrapper.builder(Person.class, outputPath)
            .charset(CharsetType.UTF_8_BOM)
            .write(persons);

        assertTrue(Files.exists(outputPath));

        // BOMが書き込まれていることを確認
        byte[] fileBytes = Files.readAllBytes(outputPath);
        assertTrue(fileBytes[0] == (byte) 0xEF && 
                   fileBytes[1] == (byte) 0xBB && 
                   fileBytes[2] == (byte) 0xBF, "BOMが書き込まれていません");

        // BOM付きで読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .charset(CharsetType.UTF_8_BOM)
            .read();

        assertEquals(1, readPersons.size());
        assertEquals("テスト太郎", readPersons.get(0).getName());
    }

    @Test
    void testBuilderWithMultipleSettings() throws IOException, CsvException {
        // Builderパターンで複数設定の組み合わせ
        Path outputPath = Paths.get("src/test/resources/builder_multi_test.tsv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("テスト太郎", 25, "エンジニア", "東京"));
        persons.add(new Person("テスト花子", 30, "デザイナー", "大阪"));

        CsvWriterWrapper.builder(Person.class, outputPath)
            .charset(CharsetType.UTF_8)
            .fileType(FileType.TSV)
            .lineSeparator(LineSeparatorType.LF)
            .useHeaderMapping()
            .write(persons);

        assertTrue(Files.exists(outputPath));

        // TSV + LFで読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .fileType(FileType.TSV)
            .read();

        assertEquals(2, readPersons.size());
        assertEquals("テスト太郎", readPersons.get(0).getName());
        assertEquals("テスト花子", readPersons.get(1).getName());
    }

    @Test
    void testBuilderWithEmptyList() throws IOException {
        // Builderパターンで空リスト
        Path outputPath = Paths.get("src/test/resources/builder_empty_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();

        CsvWriterWrapper.builder(Person.class, outputPath)
            .write(persons);

        assertTrue(Files.exists(outputPath));
    }

    @Test
    void testBuilderRoundtrip() throws IOException, CsvException {
        // Builderパターンでラウンドトリップ
        Path outputPath = Paths.get("src/test/resources/builder_roundtrip_test.csv");
        filesToDelete.add(outputPath);

        List<Person> originalPersons = new ArrayList<>();
        originalPersons.add(new Person("ラウンドトリップ太郎", 22, "学生", "千葉"));
        originalPersons.add(new Person("ラウンドトリップ花子", 24, "大学院生", "埼玉"));

        // 書き込み
        CsvWriterWrapper.builder(Person.class, outputPath)
            .write(originalPersons);

        // 読み込み
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .read();

        // データが一致することを確認
        assertEquals(originalPersons.size(), readPersons.size());
        for (int i = 0; i < originalPersons.size(); i++) {
            assertEquals(originalPersons.get(i).getName(), readPersons.get(i).getName());
            assertEquals(originalPersons.get(i).getAge(), readPersons.get(i).getAge());
        }
    }

    @Test
    void testBuilderComplexScenario() throws IOException, CsvException {
        // Builderパターンで複雑なシナリオ
        Path outputPath = Paths.get("src/test/resources/builder_complex_test.tsv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("複雑太郎", 35, "マネージャー", "福岡"));
        persons.add(new Person("複雑花子", 28, "リーダー", "広島"));
        persons.add(new Person("複雑次郎", 42, "ディレクター", "仙台"));

        // TSV + Shift_JIS + CRLF
        CsvWriterWrapper.builder(Person.class, outputPath)
            .charset(CharsetType.S_JIS)
            .fileType(FileType.TSV)
            .lineSeparator(LineSeparatorType.CRLF)
            .write(persons);

        assertTrue(Files.exists(outputPath));

        // 同じ設定で読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .charset(CharsetType.S_JIS)
            .fileType(FileType.TSV)
            .read();

        assertEquals(3, readPersons.size());
        assertEquals("複雑太郎", readPersons.get(0).getName());
        assertEquals(35, readPersons.get(0).getAge());
    }
}
