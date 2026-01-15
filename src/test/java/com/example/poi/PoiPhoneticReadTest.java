package com.example.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

import org.dhatim.fastexcel.reader.ReadableWorkbook;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("POI: フリガナ付きExcelの読み取り確認")
class PoiPhoneticReadTest {

    private static final Path SAMPLE_EXCEL_WITH_PHONETIC =
        Paths.get("src/test/resources/sample_with_phonetic.xlsx");

    @Test
    @DisplayName("POIのみでフリガナなしの値が取得できること")
    void testReadValueWithoutPhoneticUsingPoiOnly() throws IOException {
        Assumptions.assumeTrue(Files.exists(SAMPLE_EXCEL_WITH_PHONETIC),
            "フリガナ付きのExcelファイルが見つかりません: " + SAMPLE_EXCEL_WITH_PHONETIC);

        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(SAMPLE_EXCEL_WITH_PHONETIC))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet, "シートが取得できること");

            Row headerRow = null;
            Row firstDataRow = null;
            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }
                if (headerRow == null) {
                    headerRow = row;
                } else {
                    firstDataRow = row;
                    break;
                }
            }

            assertNotNull(headerRow, "ヘッダー行が存在すること");
            assertNotNull(firstDataRow, "データ行が存在すること");

            assertEquals("名前", readCellValue(headerRow.getCell(0), formatter));
            assertEquals("年齢", readCellValue(headerRow.getCell(1), formatter));
            assertEquals("職業", readCellValue(headerRow.getCell(2), formatter));
            assertEquals("出身地", readCellValue(headerRow.getCell(3), formatter));

            assertEquals("田中太郎", readCellValue(firstDataRow.getCell(0), formatter));
            assertEquals("25", readCellValue(firstDataRow.getCell(1), formatter));
            assertEquals("エンジニア", readCellValue(firstDataRow.getCell(2), formatter));
            assertEquals("東京", readCellValue(firstDataRow.getCell(3), formatter));
        }
    }

    @Test
    @DisplayName("fastexcelでフリガナなしの値が取得できること")
    void testReadValueWithoutPhoneticUsingFastExcel() throws IOException {
        Assumptions.assumeTrue(Files.exists(SAMPLE_EXCEL_WITH_PHONETIC),
            "フリガナ付きのExcelファイルが見つかりません: " + SAMPLE_EXCEL_WITH_PHONETIC);

        try (InputStream inputStream = Files.newInputStream(SAMPLE_EXCEL_WITH_PHONETIC);
             ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            org.dhatim.fastexcel.reader.Sheet sheet = workbook.getFirstSheet();
            org.dhatim.fastexcel.reader.Row headerRow = null;
            org.dhatim.fastexcel.reader.Row firstDataRow = null;

            try (Stream<org.dhatim.fastexcel.reader.Row> rows = sheet.openStream()) {
                Iterator<org.dhatim.fastexcel.reader.Row> iterator = rows.iterator();
                if (iterator.hasNext()) {
                    headerRow = iterator.next();
                }
                if (iterator.hasNext()) {
                    firstDataRow = iterator.next();
                }
            }

            assertNotNull(headerRow, "ヘッダー行が存在すること");
            assertNotNull(firstDataRow, "データ行が存在すること");

            assertEquals("名前", readFastExcelCellText(headerRow, 0));
            assertEquals("年齢", readFastExcelCellText(headerRow, 1));
            assertEquals("職業", readFastExcelCellText(headerRow, 2));
            assertEquals("出身地", readFastExcelCellText(headerRow, 3));

            assertEquals("田中太郎", readFastExcelCellText(firstDataRow, 0));
            assertEquals("25", readFastExcelCellText(firstDataRow, 1));
            assertEquals("エンジニア", readFastExcelCellText(firstDataRow, 2));
            assertEquals("東京", readFastExcelCellText(firstDataRow, 3));
        }
    }

    private static String readCellValue(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return "";
        }
        String formatted = formatter.formatCellValue(cell);
        return formatted == null ? "" : formatted.trim();
    }

    private static String readFastExcelCellText(org.dhatim.fastexcel.reader.Row row, int index) {
        if (row == null) {
            return "";
        }
        String text = row.getCellText(index);
        return text == null ? "" : text.trim();
    }
}
