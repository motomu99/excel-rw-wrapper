package com.example.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * OpenCSVをラップしたCSV読み込みクラス
 * 簡単にCSVファイルを読み込めるようにするよ〜✨
 */
public class CsvReader {

    /**
     * CSVファイルを読み込んでList<String[]>として返す
     * 
     * @param filePath CSVファイルのパス
     * @return CSVの各行データ
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    public List<String[]> readCsvFile(String filePath) throws IOException, CsvException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            return reader.readAll();
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
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            return reader.readAll();
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
        List<String[]> allData = readCsvFile(filePath);
        
        if (hasHeader && !allData.isEmpty()) {
            return allData.subList(1, allData.size());
        }
        
        return allData;
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
        List<String[]> allData = readCsvFile(filePath);
        
        if (!allData.isEmpty()) {
            return allData.get(0);
        }
        
        return new String[0];
    }
}
