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

public class CsvWriterWrapperTest {

    private List<Path> filesToDelete = new ArrayList<>();

    @AfterEach
    void cleanup() throws IOException {
        // テスト後にファイルを削除
        for (Path path : filesToDelete) {
            if (Files.exists(path)) {
                Files.delete(path);
            }
        }
        filesToDelete.clear();
    }

    @Test
    void testWriteCsvFromBeans() throws IOException, CsvException {
        // 基本的なBean書き込みテスト
        Path outputPath = Paths.get("src/test/resources/output_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("鈴木花子", 32, "デザイナー", "京都"));
        persons.add(new Person("高橋次郎", 45, "マネージャー", "福岡"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.write(persons));

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // 書き込んだファイルを読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.read());

        assertEquals(3, readPersons.size());
        assertEquals("山田太郎", readPersons.get(0).getName());
        assertEquals(28, readPersons.get(0).getAge());
        assertEquals("プログラマー", readPersons.get(0).getOccupation());
        assertEquals("神奈川", readPersons.get(0).getBirthplace());
    }

    @Test
    void testWriteCsvWithCharset() throws IOException, CsvException {
        // Shift_JISで書き込むテスト
        Path outputPath = Paths.get("src/test/resources/output_sjis_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("テスト太郎", 25, "エンジニア", "東京"));
        persons.add(new Person("テスト花子", 30, "デザイナー", "大阪"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setCharset(CharsetType.S_JIS).write(persons));

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // Shift_JISで読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setCharset(CharsetType.S_JIS).read());

        assertEquals(2, readPersons.size());
        assertEquals("テスト太郎", readPersons.get(0).getName());
        assertEquals(25, readPersons.get(0).getAge());
    }

    @Test
    void testWriteTsvFromBeans() throws IOException, CsvException {
        // TSV形式で書き込むテスト
        Path outputPath = Paths.get("src/test/resources/output_test.tsv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("TSV太郎", 35, "営業", "北海道"));
        persons.add(new Person("TSV花子", 28, "人事", "沖縄"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setFileType(FileType.TSV).write(persons));

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // TSV形式で読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setFileType(FileType.TSV).read());

        assertEquals(2, readPersons.size());
        assertEquals("TSV太郎", readPersons.get(0).getName());
        assertEquals(35, readPersons.get(0).getAge());
    }

    @Test
    void testWriteEmptyList() throws IOException, CsvException {
        // 空のリストを書き込むテスト
        Path outputPath = Paths.get("src/test/resources/output_empty_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.write(persons));

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // 読み込んで空であることを確認
        List<Person> readPersons = CsvReaderWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.read());

        assertTrue(readPersons.isEmpty());
    }

    @Test
    void testWriteCsvWithoutHeader() throws IOException, CsvException {
        // ヘッダーなしで書き込むテスト
        Path outputPath = Paths.get("src/test/resources/output_no_header_test.csv");
        filesToDelete.add(outputPath);

        List<Person2> persons = new ArrayList<>();
        persons.add(new Person2("ノーヘッダー太郎", 40, "役員", "広島"));
        persons.add(new Person2("ノーヘッダー花子", 38, "部長", "仙台"));

        CsvWriterWrapper.execute(
            Person2.class,
            outputPath,
            instance -> instance.usePositionMapping().write(persons));

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ポジションマッピングで読み込んで検証
        List<Person2> readPersons = CsvReaderWrapper.execute(
            Person2.class,
            outputPath,
            instance -> instance.usePositionMapping().read());

        assertEquals(2, readPersons.size());
        assertEquals("ノーヘッダー太郎", readPersons.get(0).getName());
        assertEquals(40, readPersons.get(0).getAge());
    }

    @Test
    void testWriteCsvWithInvalidPath() {
        // 無効なファイルパスのテスト
        Path outputPath = Paths.get("invalid/directory/does/not/exist/output.csv");

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("テスト", 20, "学生", "東京"));

        assertThrows(RuntimeException.class, () -> {
            CsvWriterWrapper.execute(
                Person.class,
                outputPath,
                instance -> instance.write(persons));
        });
    }

    @Test
    void testWriteAndReadRoundtrip() throws IOException, CsvException {
        // 書き込んで読み込んで元のデータと一致するか確認
        Path outputPath = Paths.get("src/test/resources/output_roundtrip_test.csv");
        filesToDelete.add(outputPath);

        List<Person> originalPersons = new ArrayList<>();
        originalPersons.add(new Person("ラウンドトリップ太郎", 22, "学生", "千葉"));
        originalPersons.add(new Person("ラウンドトリップ花子", 24, "大学院生", "埼玉"));
        originalPersons.add(new Person("ラウンドトリップ次郎", 26, "研究者", "茨城"));

        // 書き込み
        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.write(originalPersons));

        // 読み込み
        List<Person> readPersons = CsvReaderWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.read());

        // データが一致することを確認
        assertEquals(originalPersons.size(), readPersons.size());
        for (int i = 0; i < originalPersons.size(); i++) {
            assertEquals(originalPersons.get(i).getName(), readPersons.get(i).getName());
            assertEquals(originalPersons.get(i).getAge(), readPersons.get(i).getAge());
            assertEquals(originalPersons.get(i).getOccupation(), readPersons.get(i).getOccupation());
            assertEquals(originalPersons.get(i).getBirthplace(), readPersons.get(i).getBirthplace());
        }
    }

    @Test
    void testWriteCsvWithBom() throws IOException, CsvException {
        // BOM付きUTF-8で書き込むテスト
        Path outputPath = Paths.get("src/test/resources/output_utf8_bom_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("BOMテスト太郎", 25, "エンジニア", "東京"));
        persons.add(new Person("BOMテスト花子", 30, "デザイナー", "大阪"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setCharset(CharsetType.UTF_8_BOM).write(persons));

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // BOM付きUTF-8で読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setCharset(CharsetType.UTF_8_BOM).read());

        assertEquals(2, readPersons.size());
        assertEquals("BOMテスト太郎", readPersons.get(0).getName());
        assertEquals(25, readPersons.get(0).getAge());
        assertEquals("エンジニア", readPersons.get(0).getOccupation());
        assertEquals("東京", readPersons.get(0).getBirthplace());
    }

    @Test
    void testWriteAndReadBomRoundtrip() throws IOException, CsvException {
        // BOM付きUTF-8でラウンドトリップテスト
        Path outputPath = Paths.get("src/test/resources/output_bom_roundtrip_test.csv");
        filesToDelete.add(outputPath);

        List<Person> originalPersons = new ArrayList<>();
        originalPersons.add(new Person("日本語テスト１", 22, "学生", "千葉"));
        originalPersons.add(new Person("日本語テスト２", 24, "大学院生", "埼玉"));
        originalPersons.add(new Person("日本語テスト３", 26, "研究者", "茨城"));

        // BOM付きで書き込み
        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setCharset(CharsetType.UTF_8_BOM).write(originalPersons));

        // BOM付きで読み込み
        List<Person> readPersons = CsvReaderWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setCharset(CharsetType.UTF_8_BOM).read());

        // データが一致することを確認
        assertEquals(originalPersons.size(), readPersons.size());
        for (int i = 0; i < originalPersons.size(); i++) {
            assertEquals(originalPersons.get(i).getName(), readPersons.get(i).getName());
            assertEquals(originalPersons.get(i).getAge(), readPersons.get(i).getAge());
            assertEquals(originalPersons.get(i).getOccupation(), readPersons.get(i).getOccupation());
            assertEquals(originalPersons.get(i).getBirthplace(), readPersons.get(i).getBirthplace());
        }
    }

    @Test
    void testWriteCsvWithCrlf() throws IOException, CsvException {
        // CRLF改行コードで書き込むテスト（デフォルト）
        Path outputPath = Paths.get("src/test/resources/output_crlf_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("CRLF太郎", 25, "エンジニア", "東京"));
        persons.add(new Person("CRLF花子", 30, "デザイナー", "大阪"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setLineSeparator(LineSeparatorType.CRLF).write(persons));

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ファイルの内容にCRLF(\r\n)が含まれていることを確認
        byte[] fileBytes = Files.readAllBytes(outputPath);
        String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("\r\n"), "ファイルにCRLFが含まれていません");

        // 読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.read());

        assertEquals(2, readPersons.size());
        assertEquals("CRLF太郎", readPersons.get(0).getName());
    }

    @Test
    void testWriteCsvWithLf() throws IOException, CsvException {
        // LF改行コードで書き込むテスト
        Path outputPath = Paths.get("src/test/resources/output_lf_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("LF太郎", 35, "営業", "福岡"));
        persons.add(new Person("LF花子", 28, "人事", "札幌"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setLineSeparator(LineSeparatorType.LF).write(persons));

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ファイルの内容にLF(\n)のみが含まれ、CRLF(\r\n)が含まれていないことを確認
        byte[] fileBytes = Files.readAllBytes(outputPath);
        String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(content.contains("\r\n"), "ファイルにCRLFが含まれています");
        assertTrue(content.contains("\n"), "ファイルにLFが含まれていません");

        // 読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.read());

        assertEquals(2, readPersons.size());
        assertEquals("LF太郎", readPersons.get(0).getName());
        assertEquals(35, readPersons.get(0).getAge());
    }

    @Test
    void testWriteCsvDefaultLineSeparator() throws IOException, CsvException {
        // デフォルトがCRLFであることを確認するテスト
        Path outputPath = Paths.get("src/test/resources/output_default_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("デフォルト太郎", 40, "部長", "名古屋"));

        // 改行コードを指定せずに書き込み（デフォルトはCRLFのはず）
        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.write(persons));

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ファイルの内容にCRLF(\r\n)が含まれていることを確認（デフォルト動作）
        byte[] fileBytes = Files.readAllBytes(outputPath);
        String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("\r\n"), "デフォルトでCRLFになっていません");
    }

    // =====================================
    // 新しいBuilderパターンのテスト
    // =====================================

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
        // LFが含まれている、かつCRLFは含まれていない
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
}

