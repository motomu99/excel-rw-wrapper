package com.example.csv;

import com.example.csv.model.Person;
import com.example.csv.model.Person2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Paths;
import java.util.List;

/**
 * CsvReaderWrapperの新しいBuilderパターンAPIのテスト
 * 
 * <p>このクラスは推奨される新しいBuilderパターンのテストです。</p>
 * <p>レガシーAPIのテストは {@link CsvReaderWrapperLegacyTest} を参照してください。</p>
 */
public class CsvReaderWrapperTest {

    @Test
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
    void testBuilderWithPositionMapping() {
        // Builderパターンで位置ベースマッピング
        List<Person2> persons = CsvReaderWrapper.builder(Person2.class, Paths.get("src/test/resources/sample_no_header.csv"))
            .usePositionMapping()
            .read();
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        Person2 firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
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
    void testBuilderWithSkipLinesGreaterThanDataSize() {
        // Builderパターンでスキップ行数がデータ数以上
        List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .skipLines(10)
            .read();
       
        assertNotNull(persons);
        assertTrue(persons.isEmpty());
    }

    @Test
    void testBuilderWithFileNotFound() {
        // Builderパターンで存在しないファイル
        assertThrows(CsvReadException.class, () -> {
            CsvReaderWrapper.builder(Person.class, Paths.get("src/test/resources/nonexistent.csv"))
                .read();
        });
    }

    @Test
    void testBuilderWithInvalidPath() {
        // Builderパターンで無効なファイルパス
        assertThrows(CsvReadException.class, () -> {
            CsvReaderWrapper.builder(Person.class, Paths.get("invalid/path/file.csv"))
                .read();
        });
    }

    @Test
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
