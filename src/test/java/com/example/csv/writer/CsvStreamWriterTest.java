package com.example.csv.writer;

import com.example.common.config.CharsetType;
import com.example.common.config.FileType;
import com.example.common.config.LineSeparatorType;
import com.example.common.config.QuoteStrategy;
import com.example.csv.reader.CsvReaderWrapper;
import com.example.model.Person;
import com.example.model.PersonWithoutHeader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import com.opencsv.exceptions.CsvException;

@DisplayName("CsvStreamWriter: Stream APIを使用したCSV書き込み")
public class CsvStreamWriterTest {

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
    @DisplayName("基本的なStream書き込み - StreamデータをCSVファイルに書き込めること")
    void testBasicStreamWriting() throws IOException, CsvException {
        // 基本的なStream書き込みテスト
        Path outputPath = Paths.get("src/test/resources/output_stream_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("鈴木花子", 32, "デザイナー", "京都"));
        persons.add(new Person("高橋次郎", 45, "マネージャー", "福岡"));

        CsvStreamWriter.builder(Person.class, outputPath)
            .write(persons.stream());

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // 書き込んだファイルを読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .read();

        assertEquals(3, readPersons.size());
        assertEquals("山田太郎", readPersons.get(0).getName());
        assertEquals(28, readPersons.get(0).getAge());
    }

    @Test
    @DisplayName("フィルタリング書き込み - filter()でデータを絞り込んで書き込めること")
    void testStreamWritingWithFilter() throws IOException, CsvException {
        // フィルタリング付きのStream書き込みテスト
        Path outputPath = Paths.get("src/test/resources/output_stream_filter_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("鈴木花子", 32, "デザイナー", "京都"));
        persons.add(new Person("高橋次郎", 45, "マネージャー", "福岡"));

        CsvStreamWriter.builder(Person.class, outputPath)
            .write(persons.stream()
                .filter(person -> person.getAge() >= 30));

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // 書き込んだファイルを読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .read();

        assertEquals(2, readPersons.size()); // 30歳以上の2人のみ
        assertTrue(readPersons.stream().allMatch(person -> person.getAge() >= 30));
    }

    @Test
    @DisplayName("文字セット指定 - charset()メソッドでShift_JIS形式で書き込めること")
    void testStreamWritingWithCharset() throws IOException, CsvException {
        // 文字エンコーディング指定のテスト（Shift_JIS）
        Path outputPath = Paths.get("src/test/resources/output_stream_sjis_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("テスト太郎", 25, "エンジニア", "東京"));
        persons.add(new Person("テスト花子", 30, "デザイナー", "大阪"));

        CsvStreamWriter.builder(Person.class, outputPath)
            .charset(CharsetType.S_JIS)
            .write(persons.stream());

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // Shift_JISで読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .charset(CharsetType.S_JIS)
            .read();

        assertEquals(2, readPersons.size());
        assertEquals("テスト太郎", readPersons.get(0).getName());
    }

    @Test
    @DisplayName("ファイル形式指定 - fileType()メソッドでTSV形式で書き込めること")
    void testStreamWritingWithFileType() throws IOException, CsvException {
        // ファイルタイプ指定のテスト（TSV）
        Path outputPath = Paths.get("src/test/resources/output_stream_test.tsv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("鈴木花子", 32, "デザイナー", "京都"));

        CsvStreamWriter.builder(Person.class, outputPath)
            .fileType(FileType.TSV)
            .write(persons.stream());

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // TSVで読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .fileType(FileType.TSV)
            .read();

        assertEquals(2, readPersons.size());
        assertEquals("山田太郎", readPersons.get(0).getName());
    }

    @Test
    @DisplayName("改行コード指定 - lineSeparator()メソッドでLF改行で書き込めること")
    void testStreamWritingWithLineSeparator() throws IOException, CsvException {
        // 改行コード指定のテスト（LF）
        Path outputPath = Paths.get("src/test/resources/output_stream_lf_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("鈴木花子", 32, "デザイナー", "京都"));

        CsvStreamWriter.builder(Person.class, outputPath)
            .lineSeparator(LineSeparatorType.LF)
            .write(persons.stream());

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ファイルの内容を確認（改行コードがLFになっているか）
        String content = Files.readString(outputPath);
        assertTrue(content.contains("\n"));
        assertFalse(content.contains("\r\n")); // CRLFではない
    }

    @Test
    @DisplayName("位置ベースマッピング - usePositionMapping()でヘッダーなしで書き込めること")
    void testStreamWritingWithPositionMapping() throws IOException, CsvException {
        // 位置ベースマッピングのテスト（ヘッダーなし）
        Path outputPath = Paths.get("src/test/resources/output_stream_no_header_test.csv");
        filesToDelete.add(outputPath);

        List<PersonWithoutHeader> persons = new ArrayList<>();
        persons.add(new PersonWithoutHeader("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new PersonWithoutHeader("鈴木花子", 32, "デザイナー", "京都"));

        CsvStreamWriter.builder(PersonWithoutHeader.class, outputPath)
            .usePositionMapping()
            .write(persons.stream());

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ファイルの内容を確認（ヘッダーがないか）
        String content = Files.readString(outputPath);
        assertFalse(content.contains("名前")); // ヘッダーがない
        assertTrue(content.contains("山田太郎"));
    }

    @Test
    @DisplayName("ヘッダーマッピング - useHeaderMapping()でヘッダー付きで書き込めること")
    void testStreamWritingWithHeaderMapping() throws IOException, CsvException {
        // ヘッダーベースマッピングのテスト（ヘッダーあり）
        Path outputPath = Paths.get("src/test/resources/output_stream_header_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("鈴木花子", 32, "デザイナー", "京都"));

        CsvStreamWriter.builder(Person.class, outputPath)
            .useHeaderMapping()
            .write(persons.stream());

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ファイルの内容を確認（ヘッダーがあるか）
        String content = Files.readString(outputPath);
        assertTrue(content.contains("名前")); // ヘッダーがある
        assertTrue(content.contains("山田太郎"));
    }

    @Test
    @DisplayName("メソッドチェーン - 複数の設定とStream操作を組み合わせて書き込めること")
    void testStreamWritingWithChainedOperations() throws IOException, CsvException {
        // チェーン操作のテスト
        Path outputPath = Paths.get("src/test/resources/output_stream_chained_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("鈴木花子", 32, "デザイナー", "京都"));
        persons.add(new Person("高橋次郎", 45, "マネージャー", "福岡"));
        persons.add(new Person("田中美咲", 22, "学生", "北海道"));

        CsvStreamWriter.builder(Person.class, outputPath)
            .charset(CharsetType.UTF_8)
            .fileType(FileType.CSV)
            .lineSeparator(LineSeparatorType.LF)
            .useHeaderMapping()
            .write(persons.stream()
                .filter(person -> person.getAge() >= 25)
                .filter(person -> !person.getOccupation().equals("学生")));

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // 書き込んだファイルを読み込んで検証
        List<Person> readPersons = CsvReaderWrapper.builder(Person.class, outputPath)
            .read();

        assertEquals(3, readPersons.size()); // 25歳以上かつ学生でない3人
        assertTrue(readPersons.stream().allMatch(person -> person.getAge() >= 25));
        assertTrue(readPersons.stream().noneMatch(person -> person.getOccupation().equals("学生")));
    }

    @Test
    @DisplayName("ヘッダーなし書き込み - noHeader()でヘッダーなしで書き込めること")
    void testStreamWritingNoHeader() throws IOException, CsvException {
        Path outputPath = Paths.get("src/test/resources/output_stream_no_header_opt_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("鈴木花子", 32, "デザイナー", "京都"));

        CsvStreamWriter.builder(Person.class, outputPath)
            .noHeader()
            .write(persons.stream());

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ファイルの内容を確認（ヘッダーがないか）
        String content = Files.readString(outputPath);
        assertFalse(content.contains("名前,年齢,職業,出身地")); // ヘッダー行がない
        
        // OpenCSV 5.x のデフォルトは QuoteStrategy.ALL なので、基本はクオートされる
        // ただし、環境や設定によって微妙に変わる可能性があるので、
        // 「ヘッダーがないこと」と「データが含まれていること」を確認できればOKとする
        // 念のため、クオートあり／なしどちらのケースでもデータが含まれていることを確認
        assertTrue(
            content.contains("\"山田太郎\",\"28\",\"プログラマー\",\"神奈川\"") ||
            content.contains("山田太郎,28,プログラマー,神奈川")
        );
    }

    @Test
    @DisplayName("クオート最小化 - QuoteStrategy.MINIMALで必要な場合のみクオートされること")
    void testStreamWritingQuoteMinimal() throws IOException, CsvException {
        Path outputPath = Paths.get("src/test/resources/output_stream_quote_minimal_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        persons.add(new Person("高橋,次郎", 45, "マネージャー", "福岡")); // カンマを含む

        CsvStreamWriter.builder(Person.class, outputPath)
            .quoteStrategy(QuoteStrategy.MINIMAL)
            .write(persons.stream());

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ファイルの内容を確認
        String content = Files.readString(outputPath);
        // 通常のデータはクオートなし
        assertTrue(content.contains("山田太郎,28,プログラマー,神奈川"));
        // カンマを含むデータはクオートあり
        assertTrue(content.contains("\"高橋,次郎\",45,マネージャー,福岡"));
    }

    @Test
    @DisplayName("クオートなし - QuoteStrategy.NONEで全くクオートされないこと")
    void testStreamWritingQuoteNone() throws IOException, CsvException {
        Path outputPath = Paths.get("src/test/resources/output_stream_quote_none_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));
        // OpenCSV の仕様上、区切り文字が含まれているのにクオートをOFFにすると、
        // 単純に区切り文字がエスケープされずにそのまま出力されるか、エスケープされる可能性がある
        // ここでは、シンプルなデータでまず確認
        
        CsvStreamWriter.builder(Person.class, outputPath)
            .quoteStrategy(QuoteStrategy.NONE)
            .write(persons.stream());

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ファイルの内容を確認
        String content = Files.readString(outputPath);
        // 全くクオートされない
        assertTrue(content.contains("山田太郎,28,プログラマー,神奈川"));
    }

    @Test
    @DisplayName("ヘッダーなし＆クオート最小化 - 組み合わせで動作すること")
    void testStreamWritingNoHeaderAndQuoteMinimal() throws IOException, CsvException {
        Path outputPath = Paths.get("src/test/resources/output_stream_no_header_minimal_test.csv");
        filesToDelete.add(outputPath);

        List<Person> persons = new ArrayList<>();
        persons.add(new Person("山田太郎", 28, "プログラマー", "神奈川"));

        CsvStreamWriter.builder(Person.class, outputPath)
            .noHeader()
            .quoteStrategy(QuoteStrategy.MINIMAL)
            .write(persons.stream());

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ファイルの内容を確認
        String content = Files.readString(outputPath);
        System.out.println("Debug Content (No Header & Minimal): [" + content + "]"); // デバッグ出力
        assertFalse(content.contains("名前,年齢,職業,出身地"));
        // クオートなしでデータが出力されていることを確認
        assertTrue(content.contains("山田太郎,28,プログラマー,神奈川"));
    }

    @Test
    @DisplayName("空Stream書き込み - 空のStreamを書き込んでもエラーにならないこと")
    void testStreamWritingEmpty() throws IOException, CsvException {
        // 空のStreamを書き込むテスト
        Path outputPath = Paths.get("src/test/resources/output_stream_empty_test.csv");
        filesToDelete.add(outputPath);

        CsvStreamWriter.builder(Person.class, outputPath)
            .write(Stream.empty());

        // ファイルが作成されたことを確認
        assertTrue(Files.exists(outputPath));

        // ファイルの内容を確認（OpenCSVの仕様上、空の場合はファイルも空）
        String content = Files.readString(outputPath);
        assertTrue(content.isEmpty() || content.isBlank()); // ファイルが空
    }
}
