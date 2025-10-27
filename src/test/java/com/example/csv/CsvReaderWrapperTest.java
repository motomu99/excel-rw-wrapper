package com.example.csv;

import com.example.csv.model.Person;
import com.example.csv.model.Person2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import com.opencsv.exceptions.CsvException;

public class CsvReaderWrapperTest {

    @Test
    void testReadCsvToBeans() throws IOException, CsvException {
        // 基本的なBean読み込みの使い方

        List<Person> persons = CsvReaderWrapper.execute(
            Person.class,
            Paths.get("src/test/resources/sample.csv"),
            instance -> instance.read());
       
        assertNotNull(persons);
        assertEquals(5, persons.size()); // ヘッダーを除いた5件のデータ
        
        // 最初のPersonの確認
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
        
        // 2番目のPersonの確認
        Person secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
    void testReadCsvToBeans2() throws IOException, CsvException {
        // 基本的なBean読み込みの使い方

        List<Person> persons = CsvReaderWrapper.execute(
            Person.class,
            Paths.get("src/test/resources/sample.csv"),
            instance -> instance.setSkip(1).read());
       
        assertNotNull(persons);
        assertEquals(4, persons.size()); // ヘッダーを除いた5件のデータ
        
        // 2番目のPersonの確認
        Person secondPerson = persons.get(0);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
    void testReadCsvToBeans3() throws IOException, CsvException {
        // 基本的なBean読み込みの使い方

        List<Person> persons = CsvReaderWrapper.execute(
            Person.class,
            Paths.get("src/test/resources/sample_sjis.csv"),
            instance -> instance.setCharset(CharsetType.S_JIS).read());

        assertNotNull(persons);
        assertEquals(5, persons.size()); // ヘッダーを除いた5件のデータ
        
        // 最初のPersonの確認
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
        
        // 2番目のPersonの確認
        Person secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());

    }
    @Test
    void testReadCsvToBeans4() throws IOException, CsvException {
        // 基本的なBean読み込みの使い方

        List<Person> persons = CsvReaderWrapper.execute(
            Person.class,
            Paths.get("src/test/resources/sample.csv"),
            instance -> instance.setFileType(FileType.CSV).read());
    
        assertNotNull(persons);
        assertEquals(5, persons.size()); // ヘッダーを除いた5件のデータ
        
        // 最初のPersonの確認
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
        
        // 2番目のPersonの確認
        Person secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }
    @Test
    void testReadCsvToBeans5() throws IOException, CsvException {
        // 基本的なBean読み込みの使い方

        List<Person> persons = CsvReaderWrapper.execute(
            Person.class,
            Paths.get("src/test/resources/sample.tsv"),
            instance -> instance.setFileType(FileType.TSV).read());
    
        assertNotNull(persons);
        assertEquals(5, persons.size()); // ヘッダーを除いた5件のデータ
        
        // 最初のPersonの確認
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
        
        // 2番目のPersonの確認
        Person secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
    void testReadCsvWithSkipLinesGreaterThanDataSize() throws IOException, CsvException {
        // スキップ行数がデータ数以上のテスト
        List<Person> persons = CsvReaderWrapper.execute(
            Person.class,
            Paths.get("src/test/resources/sample.csv"),
            instance -> instance.setSkip(10).read()); // データより多いスキップ数
        
        assertNotNull(persons);
        assertTrue(persons.isEmpty()); // 空のリストが返されることを確認
    }

    @Test
    void testReadCsvWithFileNotFound() {
        // 存在しないファイルのテスト
        assertThrows(RuntimeException.class, () -> {
            CsvReaderWrapper.execute(
                Person.class,
                Paths.get("src/test/resources/nonexistent.csv"),
                instance -> instance.read());
        });
    }

    @Test
    void testReadCsvWithInvalidPath() {
        // 無効なファイルパスのテスト
        assertThrows(RuntimeException.class, () -> {
            CsvReaderWrapper.execute(
                Person.class,
                Paths.get("invalid/path/file.csv"),
                instance -> instance.read());
        });
    }
    @Test
    void testReadCsvToBeans6() throws IOException, CsvException {
        // 基本的なBean読み込みの使い方

        List<Person2> persons = CsvReaderWrapper.execute(
            Person2.class,
            Paths.get("src/test/resources/sample_no_header.csv"),
            instance -> instance.usePositionMapping().read());
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        // 最初のPersonの確認
        Person2 firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
        
        // 2番目のPersonの確認
        Person2 secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
    void testReadCsvToBeans7() throws IOException, CsvException {
        // 明示的にHeaderMappingを使用するテスト

        List<Person> persons = CsvReaderWrapper.execute(
            Person.class,
            Paths.get("src/test/resources/sample.csv"),
            instance -> instance.useHeaderMapping().read());
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        // 最初のPersonの確認
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
        
        // 2番目のPersonの確認
        Person secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
    void testUsePositionMapping() throws IOException, CsvException {
        // usePositionMapping()メソッドのテスト

        List<Person2> persons = CsvReaderWrapper.execute(
            Person2.class,
            Paths.get("src/test/resources/sample_no_header.csv"),
            instance -> instance.usePositionMapping().read());
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        // 最初のPersonの確認
        Person2 firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
    }

    @Test
    void testUseHeaderMapping() throws IOException, CsvException {
        // useHeaderMapping()メソッドのテスト

        List<Person> persons = CsvReaderWrapper.execute(
            Person.class,
            Paths.get("src/test/resources/sample.csv"),
            instance -> instance.useHeaderMapping().read());
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        // 最初のPersonの確認
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
    }

    @Test
    void testReadCsvWithBom() throws IOException, CsvException {
        // BOM付きUTF-8のCSVファイル読み込みテスト

        List<Person> persons = CsvReaderWrapper.execute(
            Person.class,
            Paths.get("src/test/resources/sample_utf8_bom.csv"),
            instance -> instance.setCharset(CharsetType.UTF_8_BOM).read());
       
        assertNotNull(persons);
        assertEquals(5, persons.size());
        
        // 最初のPersonの確認（BOMが正しく処理されていることを確認）
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
        
        // 2番目のPersonの確認
        Person secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

}
