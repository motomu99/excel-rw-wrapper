package com.example.csv;

import com.example.csv.model.Person;
import com.example.csv.model.Person2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.opencsv.exceptions.CsvException;

public class CsvStreamReaderTest {

    @Test
    void testBasicStreamProcessing() throws IOException, CsvException {
        // 基本的なStream処理のテスト
        
        List<Person> result = CsvStreamReader.of(Person.class, Paths.get("src/test/resources/sample.csv"))
            .process(stream -> stream.collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(5, result.size()); // ヘッダーを除いた5件のデータ
        
        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
    }

    @Test
    void testStreamWithFiltering() throws IOException, CsvException {
        // フィルタリング付きのStream処理のテスト
        
        List<Person> result = CsvStreamReader.of(Person.class, Paths.get("src/test/resources/sample.csv"))
            .process(stream -> stream
                .filter(person -> person.getAge() >= 30)
                .collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(2, result.size()); // 30歳以上の人は2人
        
        // 年齢の確認
        assertTrue(result.stream().allMatch(person -> person.getAge() >= 30));
    }

    @Test
    void testStreamWithMapping() throws IOException, CsvException {
        // マッピング付きのStream処理のテスト
        
        List<String> names = CsvStreamReader.of(Person.class, Paths.get("src/test/resources/sample.csv"))
            .process(stream -> stream
                .map(Person::getName)
                .collect(Collectors.toList()));
        
        assertNotNull(names);
        assertEquals(5, names.size());
        assertTrue(names.contains("田中太郎"));
        assertTrue(names.contains("佐藤花子"));
    }

    @Test
    void testStreamWithSkip() throws IOException, CsvException {
        // スキップ行数指定のテスト
        
        List<Person> result = CsvStreamReader.of(Person.class, Paths.get("src/test/resources/sample.csv"))
            .skip(2)
            .process(stream -> stream.collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(3, result.size()); // 2行スキップして3件
        
        // 最初のPersonの確認（3番目のデータ）
        Person firstPerson = result.get(0);
        assertEquals("山田次郎", firstPerson.getName());
    }

    @Test
    void testStreamWithCharset() throws IOException, CsvException {
        // 文字エンコーディング指定のテスト（Shift_JIS）
        
        List<Person> result = CsvStreamReader.of(Person.class, Paths.get("src/test/resources/sample_sjis.csv"))
            .charset(CharsetType.S_JIS)
            .process(stream -> stream.collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(5, result.size());
        
        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
    }

    @Test
    void testStreamWithFileType() throws IOException, CsvException {
        // ファイルタイプ指定のテスト（TSV）
        
        List<Person> result = CsvStreamReader.of(Person.class, Paths.get("src/test/resources/sample.tsv"))
            .fileType(FileType.TSV)
            .process(stream -> stream.collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(5, result.size());
        
        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
    }

    @Test
    void testStreamWithPositionMapping() throws IOException, CsvException {
        // 位置ベースマッピングのテスト
        
        List<Person2> result = CsvStreamReader.of(Person2.class, Paths.get("src/test/resources/sample_no_header.csv"))
            .usePositionMapping()
            .process(stream -> stream.collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(5, result.size());
        
        // 最初のPerson2の確認
        Person2 firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    void testStreamWithHeaderMapping() throws IOException, CsvException {
        // ヘッダーベースマッピングのテスト（デフォルト）
        
        List<Person> result = CsvStreamReader.of(Person.class, Paths.get("src/test/resources/sample.csv"))
            .useHeaderMapping()
            .process(stream -> stream.collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(5, result.size());
        
        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
    }

    @Test
    void testStreamWithChainedOperations() throws IOException, CsvException {
        // チェーン操作のテスト
        
        List<String> result = CsvStreamReader.of(Person.class, Paths.get("src/test/resources/sample.csv"))
            .skip(1)
            .charset(CharsetType.UTF_8)
            .fileType(FileType.CSV)
            .useHeaderMapping()
            .process(stream -> stream
                .filter(person -> person.getAge() >= 25)
                .map(Person::getName)
                .collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(3, result.size()); // 1行スキップして、25歳以上は3人
        
        assertTrue(result.contains("佐藤花子"));
        assertTrue(result.contains("山田次郎"));
        assertTrue(result.contains("高橋健太"));
    }

    @Test
    void testStreamCount() throws IOException, CsvException {
        // カウント操作のテスト
        
        Long count = CsvStreamReader.of(Person.class, Paths.get("src/test/resources/sample.csv"))
            .process(Stream::count);
        
        assertNotNull(count);
        assertEquals(5L, count);
    }

    @Test
    void testStreamForEach() throws IOException, CsvException {
        // forEach操作のテスト
        
        StringBuilder names = new StringBuilder();
        
        CsvStreamReader.of(Person.class, Paths.get("src/test/resources/sample.csv"))
            .process(stream -> {
                stream.forEach(person -> names.append(person.getName()).append(","));
                return null;
            });
        
        String result = names.toString();
        assertTrue(result.contains("田中太郎"));
        assertTrue(result.contains("佐藤花子"));
    }
}
