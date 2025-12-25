package com.example.excel.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.example.model.linenumber.PersonWithLineNumber;
import com.example.model.linenumber.PersonWithLineNumberAnnotation;
import com.example.model.linenumber.PersonWithLineNumberInterface;

/**
 * Excel読み込みでの行番号トラッキング機能のテスト
 */
class ExcelLineNumberTest {

    private static final Path EXCEL_FILE = Paths.get("src/test/resources/linenumber/person_with_line_number.xlsx");

    @Test
    void testExcelLineNumberWithAbstractClass() throws Exception {
        // LineNumberAware抽象クラスを継承したモデルのテスト
        List<PersonWithLineNumber> results = ExcelReader.builder(PersonWithLineNumber.class, EXCEL_FILE)
                .read();

        assertTrue(results.size() >= 3, "少なくとも3行のデータが必要です");

        // 1行目のデータ(ヘッダーの次の行)
        PersonWithLineNumber person1 = results.get(0);
        assertNotNull(person1.getLineNumber());
        assertTrue(person1.getLineNumber() >= 2, "行番号は2以上である必要があります");

        // 各行の行番号が連続していることを確認
        for (int i = 1; i < results.size(); i++) {
            PersonWithLineNumber current = results.get(i);
            PersonWithLineNumber previous = results.get(i - 1);
            assertEquals(previous.getLineNumber() + 1, current.getLineNumber(),
                    "行番号は連続している必要があります");
        }
    }

    @Test
    void testExcelLineNumberWithInterface() throws Exception {
        // ILineNumberAwareインターフェースを実装したモデルのテスト
        List<PersonWithLineNumberInterface> results = ExcelReader.builder(PersonWithLineNumberInterface.class, EXCEL_FILE)
                .read();

        assertTrue(results.size() >= 3);

        PersonWithLineNumberInterface person1 = results.get(0);
        assertNotNull(person1.getLineNumber());
        assertTrue(person1.getLineNumber() >= 2);

        // 各行の行番号が連続していることを確認
        for (int i = 1; i < results.size(); i++) {
            PersonWithLineNumberInterface current = results.get(i);
            PersonWithLineNumberInterface previous = results.get(i - 1);
            assertEquals(previous.getLineNumber() + 1, current.getLineNumber());
        }
    }

    @Test
    void testExcelLineNumberWithAnnotationOnly() throws Exception {
        // @LineNumberアノテーションのみを使用したモデルのテスト
        List<PersonWithLineNumberAnnotation> results = ExcelReader.builder(PersonWithLineNumberAnnotation.class, EXCEL_FILE)
                .read();

        assertTrue(results.size() >= 3);

        PersonWithLineNumberAnnotation person1 = results.get(0);
        assertNotNull(person1.getLineNumber());
        assertTrue(person1.getLineNumber() >= 2);

        // 各行の行番号が連続していることを確認
        for (int i = 1; i < results.size(); i++) {
            PersonWithLineNumberAnnotation current = results.get(i);
            PersonWithLineNumberAnnotation previous = results.get(i - 1);
            assertEquals(previous.getLineNumber() + 1, current.getLineNumber());
        }
    }

    @Test
    void testExcelLineNumberWithStream() throws Exception {
        // ExcelStreamReaderでの行番号テスト
        List<PersonWithLineNumber> results = ExcelStreamReader.builder(PersonWithLineNumber.class, EXCEL_FILE)
                .extract(stream -> stream.collect(Collectors.toList()));

        assertTrue(results.size() >= 3);

        PersonWithLineNumber person1 = results.get(0);
        assertNotNull(person1.getLineNumber());
        assertTrue(person1.getLineNumber() >= 2);

        // 各行の行番号が連続していることを確認
        for (int i = 1; i < results.size(); i++) {
            PersonWithLineNumber current = results.get(i);
            PersonWithLineNumber previous = results.get(i - 1);
            assertEquals(previous.getLineNumber() + 1, current.getLineNumber());
        }
    }
}
