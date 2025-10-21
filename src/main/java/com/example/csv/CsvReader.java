package com.example.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * OpenCSVをラップしたCSV読み込みクラス
 * 簡単にCSVファイルを読み込めるようにする
 */
@Slf4j
public class CsvReader {

    /**
     * CSVファイルを読み込んでList&lt;String[]&gt;として返す
     * 
     * @param filePath CSVファイルのパス
     * @return CSVの各行データ
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    public List<String[]> readCsvFile(String filePath) throws IOException, CsvException {
        log.info("CSVファイル読み込み開始: ファイルパス={}", filePath);
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            List<String[]> result = reader.readAll();
            log.info("CSVファイル読み込み完了: 行数={}", result.size());
            return result;
        } catch (IOException | CsvException e) {
            log.error("CSVファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * InputStreamからCSVを読み込む
     * 
     * @param inputStream CSVデータのInputStream
     * @return CSVの各行データ
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    public List<String[]> readCsvFromStream(InputStream inputStream) throws IOException, CsvException {
        log.debug("InputStreamからCSV読み込み開始");
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            List<String[]> result = reader.readAll();
            log.debug("InputStreamからCSV読み込み完了: 行数={}", result.size());
            return result;
        } catch (IOException | CsvException e) {
            log.error("InputStreamからCSV読み込み中にエラーが発生: エラー={}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * CSVファイルを読み込んで、ヘッダー行を除いたデータのみを返す
     * 
     * @param filePath CSVファイルのパス
     * @param hasHeader ヘッダー行があるかどうか
     * @return ヘッダーを除いたCSVデータ
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    public List<String[]> readCsvDataOnly(String filePath, boolean hasHeader) throws IOException, CsvException {
        log.info("CSVデータ読み込み開始（ヘッダー除外）: ファイルパス={}, ヘッダー有無={}", filePath, hasHeader);
        List<String[]> allData = readCsvFile(filePath);
        
        List<String[]> result;
        if (hasHeader && !allData.isEmpty()) {
            result = allData.subList(1, allData.size());
            log.info("ヘッダー行を除外: 元の行数={}, データ行数={}", allData.size(), result.size());
        } else {
            result = allData;
            log.info("ヘッダー行なし: データ行数={}", result.size());
        }
        
        return result;
    }

    /**
     * CSVファイルのヘッダー行を取得
     * 
     * @param filePath CSVファイルのパス
     * @return ヘッダー行のデータ
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    public String[] readCsvHeader(String filePath) throws IOException, CsvException {
        log.debug("CSVヘッダー行取得開始: ファイルパス={}", filePath);
        List<String[]> allData = readCsvFile(filePath);
        
        if (!allData.isEmpty()) {
            String[] header = allData.get(0);
            log.debug("CSVヘッダー行取得完了: ヘッダー項目数={}", header.length);
            return header;
        }
        
        log.warn("CSVファイルが空です: ファイルパス={}", filePath);
        return new String[0];
    }
}
