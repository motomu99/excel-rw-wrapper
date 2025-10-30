package com.example.csv.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.config.CharsetType;
import com.example.common.config.FileType;
import com.example.model.Person;
import com.example.model.PersonWithoutHeader;
import com.opencsv.exceptions.CsvException;

/**
 * CsvReaderWrapperのレガシーAPI（execute()メソッド）のテスト
 * 
 * <p>
 * このクラスは後方互換性を確認するためのテストです。
 * 新しいコードでは {@link CsvReaderWrapperTest} を参照してください。
 * </p>
 * 
 * @deprecated 新しいBuilderパターンのテストは {@link CsvReaderWrapperTest} を使用してください
 */
@Deprecated(since = "2.0.0")
@DisplayName("CsvReaderWrapper: レガシーAPI（互換性確認用）")
public class CsvReaderWrapperLegacyTest {

    @Test
    @DisplayName("[互換性] 基本的な読み込み - execute()メソッドでCSVファイルを読み込めること")
    void testReadCsvToBeans() throws IOException, CsvException {
        // 基本的なBean読み込みの使い方

        List<Person> persons = CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/sample.csv"),
                instance -> instance.read());

        assertNotNull(persons);
        assertEquals(5, persons.size());

        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());

        Person secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
    @DisplayName("[互換性] データ行スキップ - setSkip()メソッドが正しく動作すること")
    void testReadCsvWithSkipLines() throws IOException, CsvException {
        List<Person> persons = CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/sample.csv"),
                instance -> instance.setSkip(1).read());

        assertNotNull(persons);
        assertEquals(4, persons.size());

        Person secondPerson = persons.get(0);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
    @DisplayName("[互換性] 文字セット指定 - setCharset()メソッドが正しく動作すること")
    void testReadCsvWithCharset() throws IOException, CsvException {
        List<Person> persons = CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/sample_sjis.csv"),
                instance -> instance.setCharset(CharsetType.S_JIS).read());

        assertNotNull(persons);
        assertEquals(5, persons.size());

        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());

        Person secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
    @DisplayName("[互換性] ファイル形式指定 - setFileType()メソッドが正しく動作すること")
    void testReadCsvWithFileType() throws IOException, CsvException {
        List<Person> persons = CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/sample.csv"),
                instance -> instance.setFileType(FileType.CSV).read());

        assertNotNull(persons);
        assertEquals(5, persons.size());

        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("[互換性] TSV読み込み - setFileType(FileType.TSV)が正しく動作すること")
    void testReadTsvFile() throws IOException, CsvException {
        List<Person> persons = CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/sample.tsv"),
                instance -> instance.setFileType(FileType.TSV).read());

        assertNotNull(persons);
        assertEquals(5, persons.size());

        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("[互換性] 境界値テスト - スキップ行数がデータ数以上でも正しく動作すること")
    void testReadCsvWithSkipLinesGreaterThanDataSize() throws IOException, CsvException {
        List<Person> persons = CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/sample.csv"),
                instance -> instance.setSkip(10).read());

        assertNotNull(persons);
        assertTrue(persons.isEmpty());
    }

    @Test
    @DisplayName("[互換性] 異常系 - 存在しないファイルでRuntimeExceptionがスローされること")
    void testReadCsvWithFileNotFound() {
        assertThrows(RuntimeException.class, () -> {
            CsvReaderWrapper.execute(
                    Person.class,
                    Paths.get("src/test/resources/nonexistent.csv"),
                    instance -> instance.read());
        });
    }

    @Test
    @DisplayName("[互換性] 異常系 - 無効なパスでRuntimeExceptionがスローされること")
    void testReadCsvWithInvalidPath() {
        assertThrows(RuntimeException.class, () -> {
            CsvReaderWrapper.execute(
                    Person.class,
                    Paths.get("invalid/path/file.csv"),
                    instance -> instance.read());
        });
    }

    @Test
    @DisplayName("[互換性] 位置ベースマッピング - usePositionMapping()が正しく動作すること")
    void testReadCsvWithPositionMapping() throws IOException, CsvException {
        List<PersonWithoutHeader> persons = CsvReaderWrapper.execute(
                PersonWithoutHeader.class,
                Paths.get("src/test/resources/sample_no_header.csv"),
                instance -> instance.usePositionMapping().read());

        assertNotNull(persons);
        assertEquals(5, persons.size());

        PersonWithoutHeader firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());

        PersonWithoutHeader secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
    @DisplayName("[互換性] ヘッダーマッピング - useHeaderMapping()が正しく動作すること")
    void testReadCsvWithHeaderMapping() throws IOException, CsvException {
        List<Person> persons = CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/sample.csv"),
                instance -> instance.useHeaderMapping().read());

        assertNotNull(persons);
        assertEquals(5, persons.size());

        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("[互換性] 位置マッピング確認 - usePositionMapping()の動作確認")
    void testUsePositionMapping() throws IOException, CsvException {
        List<PersonWithoutHeader> persons = CsvReaderWrapper.execute(
                PersonWithoutHeader.class,
                Paths.get("src/test/resources/sample_no_header.csv"),
                instance -> instance.usePositionMapping().read());

        assertNotNull(persons);
        assertEquals(5, persons.size());

        PersonWithoutHeader firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("[互換性] ヘッダーマッピング確認 - useHeaderMapping()の動作確認")
    void testUseHeaderMapping() throws IOException, CsvException {
        List<Person> persons = CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/sample.csv"),
                instance -> instance.useHeaderMapping().read());

        assertNotNull(persons);
        assertEquals(5, persons.size());

        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("[互換性] BOM処理 - BOM付きUTF-8ファイルを正しく読み込めること")
    void testReadCsvWithBom() throws IOException, CsvException {
        List<Person> persons = CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/sample_utf8_bom.csv"),
                instance -> instance.setCharset(CharsetType.UTF_8_BOM).read());

        assertNotNull(persons);
        assertEquals(5, persons.size());

        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("[互換性] 改行コード対応 - LF改行のみのファイルを正しく読み込めること")
    void testReadCsvWithLf() throws IOException, CsvException {
        List<Person> persons = CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/sample_lf.csv"),
                instance -> instance.read());

        assertNotNull(persons, "personsリストがnullです");
        assertEquals(3, persons.size(), "期待される件数は3件です");

        Person firstPerson = persons.get(0);
        assertNotNull(firstPerson, "firstPersonがnullです");
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }
}
