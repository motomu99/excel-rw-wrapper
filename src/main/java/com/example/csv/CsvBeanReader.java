package com.example.csv;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * OpenCSVをラップしたBean読み込みクラス
 * アノテーションで項目名を指定してBeanにマッピングできる
 */
public class CsvBeanReader {

    /**
     * CSVファイルを読み込んでBeanのListとして返す
     * 
     * @param &lt;T&gt; Beanの型
     * @param filePath CSVファイルのパス
     * @param beanClass マッピング先のBeanクラス
     * @return BeanのList
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    public <T> List<T> readCsvToBeans(String filePath, Class<T> beanClass) throws IOException, CsvException {
        try (FileReader reader = new FileReader(filePath)) {
            return readCsvToBeansFromReader(reader, beanClass);
        }
    }

    /**
     * InputStreamからCSVを読み込んでBeanのListとして返す
     * 
     * @param &lt;T&gt; Beanの型
     * @param inputStream CSVデータのInputStream
     * @param beanClass マッピング先のBeanクラス
     * @return BeanのList
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    public <T> List<T> readCsvToBeansFromStream(InputStream inputStream, Class<T> beanClass) throws IOException, CsvException {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            return readCsvToBeansFromReader(reader, beanClass);
        }
    }

    /**
     * ReaderからCSVを読み込んでBeanのListとして返す
     * 
     * @param &lt;T&gt; Beanの型
     * @param reader CSVデータのReader
     * @param beanClass マッピング先のBeanクラス
     * @return BeanのList
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    private <T> List<T> readCsvToBeansFromReader(java.io.Reader reader, Class<T> beanClass) throws IOException, CsvException {
        // ヘッダー名でマッピングする戦略を設定
        HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(beanClass);

        // CsvToBeanBuilderでBeanにマッピング
        CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                .withMappingStrategy(strategy)
                .withIgnoreLeadingWhiteSpace(true)
                .withIgnoreEmptyLine(true)
                .build();

        return csvToBean.parse();
    }

    /**
     * CSVファイルを読み込んでBeanのListとして返す（カスタムマッピング戦略使用）
     * 
     * @param <T> Beanの型
     * @param filePath CSVファイルのパス
     * @param beanClass マッピング先のBeanクラス
     * @param strategy カスタムマッピング戦略
     * @return BeanのList
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    public <T> List<T> readCsvToBeansWithStrategy(String filePath, Class<T> beanClass, 
                                                  com.opencsv.bean.MappingStrategy<T> strategy) throws IOException, CsvException {
        try (FileReader reader = new FileReader(filePath)) {
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();

            return csvToBean.parse();
        }
    }
}
