package com.example.csv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import com.opencsv.exceptions.CsvException;

/**
 * CsvReaderのテストクラス
 * てか、これでライブラリの使い方が分かるよ〜✨
 * 
 * ## 基本的な使い方
 * 
 * ```java
 * // CsvReaderのインスタンスを作成
 * CsvReader csvReader = new CsvReader();
 * 
 * // CSVファイルを読み込み
 * List<String[]> data = csvReader.readCsvFile("path/to/your/file.csv");
 * 
 * // データを処理
 * for (String[] row : data) {
 *     for (String cell : row) {
 *         System.out.print(cell + " ");
 *     }
 *     System.out.println();
 * }
 * ```
 * 
 * ## ヘッダーを除いたデータのみを取得
 * 
 * ```java
 * // ヘッダー行を除いたデータのみを取得
 * List<String[]> dataOnly = csvReader.readCsvDataOnly("path/to/your/file.csv", true);
 * ```
 * 
 * ## ヘッダー情報のみを取得
 * 
 * ```java
 * // ヘッダー行のみを取得
 * String[] header = csvReader.readCsvHeader("path/to/your/file.csv");
 * ```
 * 
 * ## InputStreamから読み込み
 * 
 * ```java
 * // InputStreamからCSVを読み込み
 * List<String[]> data = csvReader.readCsvFromStream(inputStream);
 * ```
 */
public class CsvReaderTest {

    private CsvReader csvReader;

    @BeforeEach
    void setUp() {
        csvReader = new CsvReader();
    }

    @Test
    void testReadCsvFile() throws IOException, CsvException {
        // 基本的なCSV読み込みの使い方
        // CsvReader csvReader = new CsvReader();
        // List<String[]> data = csvReader.readCsvFile("path/to/your/file.csv");
        
        // 日本語サンプルCSVの読み込みテスト
        List<String[]> data = csvReader.readCsvFile("src/test/resources/sample.csv");
        
        assertNotNull(data);
        assertFalse(data.isEmpty());
        assertEquals(6, data.size()); // ヘッダー + 5行のデータ
        
        // ヘッダーの確認
        String[] header = data.get(0);
        assertEquals("名前", header[0]);
        assertEquals("年齢", header[1]);
        assertEquals("職業", header[2]);
        assertEquals("出身地", header[3]);
        
        // データの確認
        String[] firstRow = data.get(1);
        assertEquals("田中太郎", firstRow[0]);
        assertEquals("25", firstRow[1]);
        assertEquals("エンジニア", firstRow[2]);
        assertEquals("東京", firstRow[3]);
    }

    @Test
    void testReadCsvDataOnly() throws IOException, CsvException {
        // ヘッダーを除いたデータのみを取得する使い方
        // List<String[]> dataOnly = csvReader.readCsvDataOnly("path/to/your/file.csv", true);
        
        // ヘッダーを除いたデータのみを取得
        List<String[]> dataOnly = csvReader.readCsvDataOnly("src/test/resources/sample.csv", true);
        
        assertNotNull(dataOnly);
        assertEquals(5, dataOnly.size()); // ヘッダーを除いた5行のデータ
        
        // 最初の行がヘッダーではないことを確認
        String[] firstRow = dataOnly.get(0);
        assertEquals("田中太郎", firstRow[0]);
    }

    @Test
    void testReadCsvHeader() throws IOException, CsvException {
        // ヘッダー情報のみを取得する使い方
        // String[] header = csvReader.readCsvHeader("path/to/your/file.csv");
        
        // ヘッダー行のみを取得
        String[] header = csvReader.readCsvHeader("src/test/resources/sample.csv");
        
        assertNotNull(header);
        assertEquals(4, header.length);
        assertEquals("名前", header[0]);
        assertEquals("年齢", header[1]);
        assertEquals("職業", header[2]);
        assertEquals("出身地", header[3]);
    }

    @Test
    void testReadEmployeesCsv() throws IOException, CsvException {
        // 英語サンプルCSVの読み込みテスト
        List<String[]> data = csvReader.readCsvFile("src/test/resources/employees.csv");
        
        assertNotNull(data);
        assertEquals(6, data.size()); // ヘッダー + 5行のデータ
        
        // ヘッダーの確認
        String[] header = data.get(0);
        assertEquals("id", header[0]);
        assertEquals("name", header[1]);
        assertEquals("department", header[2]);
        assertEquals("salary", header[3]);
        assertEquals("hire_date", header[4]);
        
        // データの確認
        String[] firstRow = data.get(1);
        assertEquals("1", firstRow[0]);
        assertEquals("John Smith", firstRow[1]);
        assertEquals("Engineering", firstRow[2]);
        assertEquals("75000", firstRow[3]);
        assertEquals("2020-01-15", firstRow[4]);
    }

    @Test
    void testReadNonExistentFile() {
        // 存在しないファイルの読み込みテスト
        assertThrows(IOException.class, () -> {
            csvReader.readCsvFile("non-existent-file.csv");
        });
    }

    @Test
    void testReadCsvDataOnlyWithoutHeader() throws IOException, CsvException {
        // ヘッダーがない場合のテスト
        List<String[]> dataOnly = csvReader.readCsvDataOnly("src/test/resources/sample.csv", false);
        
        assertNotNull(dataOnly);
        assertEquals(6, dataOnly.size()); // ヘッダーを含む全データ
        
        // 最初の行がヘッダーであることを確認
        String[] firstRow = dataOnly.get(0);
        assertEquals("名前", firstRow[0]);
    }

    @Test
    void testReadCsvHeaderFromEmptyFile() {
        // 空のファイルのヘッダー取得テスト
        assertThrows(IOException.class, () -> {
            csvReader.readCsvHeader("non-existent-file.csv");
        });
    }

    @Test
    void testReadCsvDataOnlyFromEmptyFile() {
        // 空のファイルのデータ取得テスト
        assertThrows(IOException.class, () -> {
            csvReader.readCsvDataOnly("non-existent-file.csv", true);
        });
    }
}
