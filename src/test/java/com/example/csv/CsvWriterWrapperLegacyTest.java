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
 * CsvWriterWrapperのレガシーAPI（execute()メソッド）のテスト
 * 
 * <p>このクラスは後方互換性を確認するためのテストです。
 * 新しいコードでは {@link CsvWriterWrapperTest} を参照してください。</p>
 * 
 * @deprecated 新しいBuilderパターンのテストは {@link CsvWriterWrapperTest} を使用してください
 */
@Deprecated(since = "2.0.0")
public class CsvWriterWrapperLegacyTest {

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
    void testWriteCsvFromBeans() throws IOException, CsvException {
        Path outputPath = Paths.get("src/test/resources/legacy_output_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("鈴木花子", 32, "デザイナー", "京都"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.write(persons));

        assertTrue(Files.exists(outputPath));

        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath).read();
        assertEquals(2, readPersons.size());
        assertEquals("山田太郎", readPersons.get(0).getName());
    }

    @Test
    void testWriteCsvWithCharset() throws IOException, CsvException {
        Path outputPath = Paths.get("src/test/resources/legacy_output_sjis_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("テスト太郎", 25, "エンジニア", "東京"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setCharset(CharsetType.S_JIS).write(persons));

        assertTrue(Files.exists(outputPath));

        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .charset(CharsetType.S_JIS)
            .read();
        assertEquals(1, readPersons.size());
        assertEquals("テスト太郎", readPersons.get(0).getName());
    }

    @Test
    void testWriteTsvFromBeans() throws IOException, CsvException {
        Path outputPath = Paths.get("src/test/resources/legacy_output_test.tsv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("TSV太郎", 35, "営業", "北海道"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setFileType(FileType.TSV).write(persons));

        assertTrue(Files.exists(outputPath));
    }

    @Test
    void testWriteEmptyList() throws IOException {
        Path outputPath = Paths.get("src/test/resources/legacy_output_empty_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.write(persons));

        assertTrue(Files.exists(outputPath));
    }

    @Test
    void testWriteCsvWithoutHeader() throws IOException, CsvException {
        Path outputPath = Paths.get("src/test/resources/legacy_output_no_header_test.csv");
        filesToDelete.add(outputPath);

        List<Person2> persons = new ArrayList<>();
        persons.add(new Person2("ノーヘッダー太郎", 40, "役員", "広島"));

        CsvWriterWrapper.execute(
            Person2.class,
            outputPath,
            instance -> instance.usePositionMapping().write(persons));

        assertTrue(Files.exists(outputPath));
    }

    @Test
    void testWriteCsvWithInvalidPath() {
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
    void testWriteCsvWithBom() throws IOException, CsvException {
        Path outputPath = Paths.get("src/test/resources/legacy_output_utf8_bom_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("BOMテスト太郎", 25, "エンジニア", "東京"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setCharset(CharsetType.UTF_8_BOM).write(persons));

        assertTrue(Files.exists(outputPath));

        byte[] fileBytes = Files.readAllBytes(outputPath);
        assertTrue(fileBytes[0] == (byte) 0xEF && 
                   fileBytes[1] == (byte) 0xBB && 
                   fileBytes[2] == (byte) 0xBF, "BOMが書き込まれていません");
    }

    @Test
    void testWriteCsvWithCrlf() throws IOException {
        Path outputPath = Paths.get("src/test/resources/legacy_output_crlf_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("CRLF太郎", 25, "エンジニア", "東京"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setLineSeparator(LineSeparatorType.CRLF).write(persons));

        assertTrue(Files.exists(outputPath));

        byte[] fileBytes = Files.readAllBytes(outputPath);
        String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("\r\n"), "ファイルにCRLFが含まれていません");
    }

    @Test
    void testWriteCsvWithLf() throws IOException {
        Path outputPath = Paths.get("src/test/resources/legacy_output_lf_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("LF太郎", 35, "営業", "福岡"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.setLineSeparator(LineSeparatorType.LF).write(persons));

        assertTrue(Files.exists(outputPath));

        byte[] fileBytes = Files.readAllBytes(outputPath);
        String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(content.contains("\r\n"), "ファイルにCRLFが含まれています");
        assertTrue(content.contains("\n"), "ファイルにLFが含まれていません");
    }

    @Test
    void testWriteCsvDefaultLineSeparator() throws IOException {
        Path outputPath = Paths.get("src/test/resources/legacy_output_default_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("デフォルト太郎", 40, "部長", "名古屋"));

        CsvWriterWrapper.execute(
            Person.class,
            outputPath,
            instance -> instance.write(persons));

        assertTrue(Files.exists(outputPath));

        byte[] fileBytes = Files.readAllBytes(outputPath);
        String content = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("\r\n"), "デフォルトでCRLFになっていません");
    }
}

