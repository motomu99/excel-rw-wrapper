package com.example.csv.reader;

import com.example.common.config.CharsetType;
import com.example.common.config.FileType;
import com.example.model.Person;
import com.example.model.PersonWithoutHeader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Consumer;
import java.util.function.Function;
import com.opencsv.exceptions.CsvException;
import com.example.exception.CsvReadException;

@DisplayName("CsvStreamReader: Stream APIを使用したCSV読み込み")
public class CsvStreamReaderTest {

    @Test
    @DisplayName("基本的なStream処理 - CSVファイルをStreamとして読み込み、Listに変換できること")
    void testBasicStreamProcessing() throws IOException, CsvException {
        // 基本的なStream処理のテスト
        
        List<Person> result = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .extract(stream -> stream.collect(Collectors.toList()));
        
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
    @DisplayName("フィルタリング - Stream.filter()を使用してデータを絞り込めること")
    void testStreamWithFiltering() throws IOException, CsvException {
        // フィルタリング付きのStream処理のテスト
        
        List<Person> result = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .extract(stream -> stream
                .filter(person -> person.getAge() >= 30)
                .collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(2, result.size()); // 30歳以上の人は2人
        
        // 年齢の確認
        assertTrue(result.stream().allMatch(person -> person.getAge() >= 30));
    }

    @Test
    @DisplayName("マッピング - Stream.map()を使用してデータを変換できること")
    void testStreamWithMapping() throws IOException, CsvException {
        // マッピング付きのStream処理のテスト
        
        List<String> names = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .extract(stream -> stream
                .map(Person::getName)
                .collect(Collectors.toList()));
        
        assertNotNull(names);
        assertEquals(5, names.size());
        assertTrue(names.contains("田中太郎"));
        assertTrue(names.contains("佐藤花子"));
    }

    @Test
    @DisplayName("行スキップ - skip()メソッドで指定行数をスキップできること")
    void testStreamWithSkip() throws IOException, CsvException {
        // スキップ行数指定のテスト
        
        List<Person> result = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .skip(2)
            .extract(stream -> stream.collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(3, result.size()); // 2行スキップして3件
        
        // 最初のPersonの確認（3番目のデータ）
        Person firstPerson = result.get(0);
        assertEquals("山田次郎", firstPerson.getName());
    }

    @Test
    @DisplayName("文字セット指定 - charset()メソッドでShift_JISファイルを読み込めること")
    void testStreamWithCharset() throws IOException, CsvException {
        // 文字エンコーディング指定のテスト（Shift_JIS）
        
        List<Person> result = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample_sjis.csv"))
            .charset(CharsetType.S_JIS)
            .extract(stream -> stream.collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(5, result.size());
        
        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
    }

    @Test
    @DisplayName("ファイル形式指定 - fileType()メソッドでTSVファイルを読み込めること")
    void testStreamWithFileType() throws IOException, CsvException {
        // ファイルタイプ指定のテスト（TSV）
        
        List<Person> result = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.tsv"))
            .fileType(FileType.TSV)
            .extract(stream -> stream.collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(5, result.size());
        
        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
    }

    @Test
    @DisplayName("位置ベースマッピング - usePositionMapping()でヘッダーなしCSVを読み込めること")
    void testStreamWithPositionMapping() throws IOException, CsvException {
        // 位置ベースマッピングのテスト
        
        List<PersonWithoutHeader> result = CsvStreamReader.builder(PersonWithoutHeader.class, Paths.get("src/test/resources/sample_no_header.csv"))
            .usePositionMapping()
            .extract(stream -> stream.collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(5, result.size());
        
        // 最初のPersonWithoutHeaderの確認
        PersonWithoutHeader firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
    }

    @Test
    @DisplayName("ヘッダーマッピング - useHeaderMapping()でヘッダー付きCSVを読み込めること")
    void testStreamWithHeaderMapping() throws IOException, CsvException {
        // ヘッダーベースマッピングのテスト（デフォルト）
        
        List<Person> result = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .useHeaderMapping()
            .extract(stream -> stream.collect(Collectors.toList()));
        
        assertNotNull(result);
        assertEquals(5, result.size());
        
        // 最初のPersonの確認
        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
    }

    @Test
    @DisplayName("メソッドチェーン - 複数の設定とStream操作を組み合わせて使用できること")
    void testStreamWithChainedOperations() throws IOException, CsvException {
        // チェーン操作のテスト
        
        List<String> result = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .skip(1)
            .charset(CharsetType.UTF_8)
            .fileType(FileType.CSV)
            .useHeaderMapping()
            .extract(stream -> stream
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
    @DisplayName("カウント操作 - Stream.count()でレコード数を取得できること")
    void testStreamCount() throws IOException, CsvException {
        // カウント操作のテスト
        
        Long count = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .extract(Stream::count);
        
        assertNotNull(count);
        assertEquals(5L, count);
    }

    @Test
    @DisplayName("forEach操作 - Stream.forEach()で各レコードを処理できること")
    void testStreamForEach() throws IOException, CsvException {
        // forEach操作のテスト
        
        StringBuilder names = new StringBuilder();
        
        CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .consume(stream -> {
                stream.forEach(person -> names.append(person.getName()).append(","));
            });
        
        String result = names.toString();
        assertTrue(result.contains("田中太郎"));
        assertTrue(result.contains("佐藤花子"));
    }

    @Test
    @DisplayName("Consumerオーバーロード - 戻り値なしで副作用処理ができること")
    void testConsumerOverloadBasic() throws IOException, CsvException {
        StringBuilder names = new StringBuilder();

        CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .consume(stream -> {
                stream.map(Person::getName).forEach(name -> names.append(name).append(","));
            });

        String result = names.toString();
        assertTrue(result.contains("田中太郎"));
        assertTrue(result.contains("佐藤花子"));
        assertTrue(result.contains("山田次郎"));
    }

    @Test
    @DisplayName("Consumerオーバーロード + skip - 行スキップ後に副作用処理できること")
    void testConsumerOverloadWithSkip() throws IOException, CsvException {
        StringBuilder names = new StringBuilder();

        CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
            .skip(2)
            .consume(stream -> stream.forEach(person -> names.append(person.getName()).append(",")));

        String result = names.toString();
        assertFalse(result.contains("田中太郎")); // スキップされているはず
        assertFalse(result.contains("佐藤花子")); // スキップされているはず
        assertTrue(result.contains("山田次郎"));
    }

    @Test
    @DisplayName("異常系 - 存在しないファイルの場合、CsvReadExceptionをスローすること")
    void testStreamReaderWithFileNotFound() {
        // Stream APIで存在しないファイル
        assertThrows(CsvReadException.class, () -> {
            CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/nonexistent.csv"))
                .extract(stream -> stream.collect(Collectors.toList()));
        });
    }

    @Test
    @DisplayName("異常系 - 文字コード自動判別時にファイルが存在しない場合、CsvReadExceptionをスローすること")
    void testStreamReaderWithFileNotFoundDuringCharsetDetection() {
        // 文字コードを明示的に指定せず（自動判別）、存在しないファイルを読み込もうとした場合
        CsvReadException exception = assertThrows(CsvReadException.class, () -> {
            CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/nonexistent.csv"))
                // charsetTypeを指定しない = 自動判別が実行される
                .extract(stream -> stream.collect(Collectors.toList()));
        });
        
        // エラーメッセージにファイルパスが含まれていることを確認
        assertTrue(exception.getMessage().contains("nonexistent.csv"), 
            "エラーメッセージにファイルパスが含まれていること");
    }
}
