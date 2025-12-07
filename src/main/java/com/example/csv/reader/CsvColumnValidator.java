package com.example.csv.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;

import com.example.common.util.BomHandler;
import com.example.exception.CsvReadException;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.enums.CSVReaderNullFieldIndicator;
import com.opencsv.exceptions.CsvValidationException;

/**
 * CSV/TSVファイルの列数を検証するユーティリティ。
 */
final class CsvColumnValidator {

    private CsvColumnValidator() {
        // ユーティリティクラス
    }

    static void validate(Path filePath, Charset charset, boolean withBom, char delimiter) {
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(delimiter)
                .withQuoteChar('"')
                .withIgnoreQuotations(false)
                .withIgnoreLeadingWhiteSpace(true)
                .build();

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             InputStream is = withBom ? BomHandler.skipBom(fis) : fis;
             InputStreamReader isr = new InputStreamReader(is, charset);
             CSVReader reader = new CSVReaderBuilder(isr)
                     .withCSVParser(parser)
                     .withFieldAsNull(CSVReaderNullFieldIndicator.NEITHER)
                     .build()) {

            String[] row;
            int expectedColumns = -1;
            int lineNumber = 0;

            row = reader.readNext();
            while (row != null) {
                if (isRowEmpty(row)) {
                    continue;
                }
                lineNumber++;

                if (expectedColumns < 0) {
                    expectedColumns = row.length;
                    continue;
                }

                if (row.length != expectedColumns) {
                    throw new CsvReadException(buildMismatchMessage(filePath, lineNumber, expectedColumns, row.length, row));
                }
                row = reader.readNext();
            }
        } catch (IOException | CsvValidationException e) {
            throw new CsvReadException("CSV列数の検証に失敗しました: " + filePath, e);
        }
    }

    private static boolean isRowEmpty(String[] row) {
        return row.length == 1 && row[0].isEmpty();
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private static String buildMismatchMessage(Path filePath, int lineNumber, int expected, int actual, String[] row) {
        String preview = String.join(",", row);
        // プレビュー文字列の最大長（意図が明確なリテラル使用）
        final int maxPreviewLength = 120;
        final int truncateLength = 117;
        if (preview.length() > maxPreviewLength) {
            preview = preview.substring(0, truncateLength) + "...";
        }
        return String.format("列数が不一致です (ファイル=%s, 行番号=%d, 期待値=%d, 実際=%d, 行内容=%s)",
                filePath, lineNumber, expected, actual, preview);
    }
}

