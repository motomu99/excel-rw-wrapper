package com.example.excel.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.model.Person;

@DisplayName("ExcelStreamReader: フリガナ付きヘッダー読み取り")
class ExcelStreamReaderPhoneticTest {

    private static final Path SAMPLE_EXCEL_WITH_PHONETIC =
        Paths.get("src/test/resources/sample_with_phonetic.xlsx");

    @Test
    @DisplayName("フリガナ付きExcelでも見た目の値で取得できること")
    void testReadWithoutPhonetic() throws IOException {
        Assumptions.assumeTrue(Files.exists(SAMPLE_EXCEL_WITH_PHONETIC),
            "フリガナ付きのExcelファイルが見つかりません: " + SAMPLE_EXCEL_WITH_PHONETIC);

        List<Person> result = ExcelStreamReader.builder(Person.class, SAMPLE_EXCEL_WITH_PHONETIC)
            .useHeaderMapping()
            .extract(stream -> stream.collect(Collectors.toList()));

        assertNotNull(result);
        assertEquals(3, result.size());

        Person firstPerson = result.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
    }
}
