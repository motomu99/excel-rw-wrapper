package com.example.csv.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.example.common.model.RowData;
import com.example.model.linenumber.PersonWithLineNumber;
import com.example.model.linenumber.PersonWithLineNumberAnnotation;
import com.example.model.linenumber.PersonWithLineNumberInterface;
import com.example.model.linenumber.PersonWithoutHeaderAndLineNumber;

/**
 * 行番号トラッキング機能のテスト
 */
class LineNumberTest {

    private static final Path CSV_FILE = Paths.get("src/test/resources/linenumber/person_with_line_number.csv");
    private static final Path CSV_FILE_NO_HEADER = Paths.get("src/test/resources/linenumber/person_no_header.csv");

    @Test
    void testLineNumberWithAbstractClass() {
        // LineNumberAware抽象クラスを継承したモデルのテスト
        List<PersonWithLineNumber> results = CsvReaderWrapper.builder(PersonWithLineNumber.class, CSV_FILE)
                .read();

        assertEquals(3, results.size());

        // 1行目のデータ(ヘッダーの次の行 = 行番号2)
        PersonWithLineNumber person1 = results.get(0);
        assertEquals(2, person1.getLineNumber());
        assertEquals("太郎", person1.getName());
        assertEquals(25, person1.getAge());
        assertEquals("エンジニア", person1.getOccupation());

        // 2行目のデータ(行番号3)
        PersonWithLineNumber person2 = results.get(1);
        assertEquals(3, person2.getLineNumber());
        assertEquals("花子", person2.getName());
        assertEquals(30, person2.getAge());
        assertEquals("デザイナー", person2.getOccupation());

        // 3行目のデータ(行番号4)
        PersonWithLineNumber person3 = results.get(2);
        assertEquals(4, person3.getLineNumber());
        assertEquals("次郎", person3.getName());
        assertEquals(28, person3.getAge());
        assertEquals("マネージャー", person3.getOccupation());
    }

    @Test
    void testLineNumberWithInterface() {
        // ILineNumberAwareインターフェースを実装したモデルのテスト
        List<PersonWithLineNumberInterface> results = CsvReaderWrapper.builder(PersonWithLineNumberInterface.class, CSV_FILE)
                .read();

        assertEquals(3, results.size());

        PersonWithLineNumberInterface person1 = results.get(0);
        assertEquals(2, person1.getLineNumber());
        assertEquals("太郎", person1.getName());

        PersonWithLineNumberInterface person2 = results.get(1);
        assertEquals(3, person2.getLineNumber());
        assertEquals("花子", person2.getName());

        PersonWithLineNumberInterface person3 = results.get(2);
        assertEquals(4, person3.getLineNumber());
        assertEquals("次郎", person3.getName());
    }

    @Test
    void testLineNumberWithAnnotationOnly() {
        // @LineNumberアノテーションのみを使用したモデルのテスト
        List<PersonWithLineNumberAnnotation> results = CsvReaderWrapper.builder(PersonWithLineNumberAnnotation.class, CSV_FILE)
                .read();

        assertEquals(3, results.size());

        PersonWithLineNumberAnnotation person1 = results.get(0);
        assertEquals(2, person1.getLineNumber());
        assertEquals("太郎", person1.getName());

        PersonWithLineNumberAnnotation person2 = results.get(1);
        assertEquals(3, person2.getLineNumber());
        assertEquals("花子", person2.getName());

        PersonWithLineNumberAnnotation person3 = results.get(2);
        assertEquals(4, person3.getLineNumber());
        assertEquals("次郎", person3.getName());
    }

    @Test
    void testLineNumberWithStream() throws Exception {
        // CsvStreamReaderでの行番号テスト
        List<PersonWithLineNumber> results = CsvStreamReader.builder(PersonWithLineNumber.class, CSV_FILE)
                .extract(stream -> stream.collect(Collectors.toList()));

        assertEquals(3, results.size());

        PersonWithLineNumber person1 = results.get(0);
        assertEquals(2, person1.getLineNumber());
        assertEquals("太郎", person1.getName());

        PersonWithLineNumber person2 = results.get(1);
        assertEquals(3, person2.getLineNumber());
        assertEquals("花子", person2.getName());

        PersonWithLineNumber person3 = results.get(2);
        assertEquals(4, person3.getLineNumber());
        assertEquals("次郎", person3.getName());
    }

    @Test
    void testLineNumberWithSkipLines() {
        // skipLinesと併用した場合のテスト
        List<PersonWithLineNumber> results = CsvReaderWrapper.builder(PersonWithLineNumber.class, CSV_FILE)
                .skipLines(1)
                .read();

        assertEquals(2, results.size());

        // skipLinesで1行スキップしたが、行番号は元ファイルの行番号のまま
        PersonWithLineNumber person1 = results.get(0);
        assertEquals(3, person1.getLineNumber()); // 2行目をスキップしたので3行目から
        assertEquals("花子", person1.getName());

        PersonWithLineNumber person2 = results.get(1);
        assertEquals(4, person2.getLineNumber());
        assertEquals("次郎", person2.getName());
    }

    @Test
    void testLineNumberWithoutHeader() {
        // ヘッダーなしCSVファイルの行番号テスト（位置ベースマッピング）
        List<PersonWithoutHeaderAndLineNumber> results = CsvReaderWrapper.builder(PersonWithoutHeaderAndLineNumber.class, CSV_FILE_NO_HEADER)
                .read();

        assertEquals(3, results.size());

        // ヘッダーなしの場合、1行目からデータが始まる（行番号1）
        PersonWithoutHeaderAndLineNumber person1 = results.get(0);
        assertEquals(1, person1.getLineNumber());
        assertEquals("太郎", person1.getName());
        assertEquals(25, person1.getAge());
        assertEquals("エンジニア", person1.getOccupation());

        // 2行目のデータ（行番号2）
        PersonWithoutHeaderAndLineNumber person2 = results.get(1);
        assertEquals(2, person2.getLineNumber());
        assertEquals("花子", person2.getName());
        assertEquals(30, person2.getAge());
        assertEquals("デザイナー", person2.getOccupation());

        // 3行目のデータ（行番号3）
        PersonWithoutHeaderAndLineNumber person3 = results.get(2);
        assertEquals(3, person3.getLineNumber());
        assertEquals("次郎", person3.getName());
        assertEquals(28, person3.getAge());
        assertEquals("マネージャー", person3.getOccupation());
    }

    @Test
    void testLineNumberWithoutHeaderStream() throws Exception {
        // CsvStreamReaderでヘッダーなしCSVの行番号テスト
        List<PersonWithoutHeaderAndLineNumber> results = CsvStreamReader.builder(PersonWithoutHeaderAndLineNumber.class, CSV_FILE_NO_HEADER)
                .extract(stream -> stream.collect(Collectors.toList()));

        assertEquals(3, results.size());

        PersonWithoutHeaderAndLineNumber person1 = results.get(0);
        assertEquals(1, person1.getLineNumber());
        assertEquals("太郎", person1.getName());

        PersonWithoutHeaderAndLineNumber person2 = results.get(1);
        assertEquals(2, person2.getLineNumber());
        assertEquals("花子", person2.getName());

        PersonWithoutHeaderAndLineNumber person3 = results.get(2);
        assertEquals(3, person3.getLineNumber());
        assertEquals("次郎", person3.getName());
    }

    @Test
    void testRowData() {
        // RowDataを使用した行番号取得のテスト
        List<RowData<com.example.model.Person>> results = CsvReaderWrapper.builder(com.example.model.Person.class, CSV_FILE)
                .readWithLineNumber();

        assertEquals(3, results.size());

        // 1行目のデータ(ヘッダーの次の行 = 行番号2)
        RowData<com.example.model.Person> row1 = results.get(0);
        assertEquals(2, row1.getLineNumber());
        assertEquals("太郎", row1.getData().getName());
        assertEquals(25, row1.getData().getAge());

        // 2行目のデータ(行番号3)
        RowData<com.example.model.Person> row2 = results.get(1);
        assertEquals(3, row2.getLineNumber());
        assertEquals("花子", row2.getData().getName());

        // 3行目のデータ(行番号4)
        RowData<com.example.model.Person> row3 = results.get(2);
        assertEquals(4, row3.getLineNumber());
        assertEquals("次郎", row3.getData().getName());
    }
}
