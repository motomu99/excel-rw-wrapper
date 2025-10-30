package com.example.csv.reader;

import com.example.common.config.CharsetType;
import com.example.common.config.FileType;
import com.example.csv.reader.CsvReaderWrapper;
import com.example.exception.CsvReadException;
import com.example.model.Person;
import com.example.model.PersonWithoutHeader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

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
}
