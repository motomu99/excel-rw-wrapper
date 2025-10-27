package com.example.csv;

import com.example.csv.model.Person;
import com.example.csv.model.Person2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import com.opencsv.exceptions.CsvException;

/**
 * CsvReaderWrapperのレガシーAPI（execute()メソッド）のテスト
 * 
 * <p>このクラスは後方互換性を確認するためのテストです。
 * 新しいコードでは {@link CsvReaderWrapperTest} を参照してください。</p>
 * 
 * @deprecated 新しいBuilderパターンのテストは {@link CsvReaderWrapperTest} を使用してください
 */
@Deprecated(since = "2.0.0")
public class CsvReaderWrapperLegacyTest {

    @Test
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
    void testReadCsvWithSkipLinesGreaterThanDataSize() throws IOException, CsvException {
        List<Person> persons = CsvReaderWrapper.execute(
            Person.class,
            Paths.get("src/test/resources/sample.csv"),
            instance -> instance.setSkip(10).read());
        
        assertNotNull(persons);
        assertTrue(persons.isEmpty());
    }

    @Test
    void testReadCsvWithFileNotFound() {
        assertThrows(RuntimeException.class, () -> {
            CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/nonexistent.csv"),
                instance -> instance.read());
        });
    }

    @Test
    void testReadCsvWithInvalidPath() {
        assertThrows(RuntimeException.class, () -> {
            CsvReaderWrapper.execute(
                Person.class,
                Paths.get("invalid/path/file.csv"),
                instance -> instance.read());
        });
    }

    @Test
    void testReadCsvWithPositionMapping() throws IOException, CsvException {
        List<Person2> persons = CsvReaderWrapper.execute(
            Person2.class,
            Paths.get("src/test/resources/sample_no_header.csv"),
            instance -> instance.usePositionMapping().read());
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        Person2 firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
        
        Person2 secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
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
    void testUsePositionMapping() throws IOException, CsvException {
        List<Person2> persons = CsvReaderWrapper.execute(
            Person2.class,
            Paths.get("src/test/resources/sample_no_header.csv"),
            instance -> instance.usePositionMapping().read());
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        Person2 firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
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

