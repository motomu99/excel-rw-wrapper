package com.example.csv;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/**
 * OpenCSVをラップしたBean読み込みクラス
 * アノテーションで項目名を指定してBeanにマッピングできる
 */
@Slf4j
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
        log.info("CSVファイルからBean読み込み開始: ファイルパス={}, Beanクラス={}", filePath, beanClass.getSimpleName());
        try (FileReader reader = new FileReader(filePath)) {
            List<T> result = readCsvToBeansFromReader(reader, beanClass);
            log.info("CSVファイルからBean読み込み完了: 読み込み件数={}", result.size());
            return result;
        } catch (IOException | CsvException e) {
            log.error("CSVファイルからBean読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw e;
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
        log.debug("InputStreamからBean読み込み開始: Beanクラス={}", beanClass.getSimpleName());
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            List<T> result = readCsvToBeansFromReader(reader, beanClass);
            log.debug("InputStreamからBean読み込み完了: 読み込み件数={}", result.size());
            return result;
        } catch (IOException | CsvException e) {
            log.error("InputStreamからBean読み込み中にエラーが発生: エラー={}", e.getMessage(), e);
            throw e;
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
        log.debug("ReaderからBeanマッピング開始: Beanクラス={}", beanClass.getSimpleName());
        
        // ヘッダー名でマッピングする戦略を設定
        HeaderColumnNameMappingStrategy<T> strategy = new HeaderColumnNameMappingStrategy<>();
        strategy.setType(beanClass);
        log.debug("マッピング戦略設定完了: 戦略={}", strategy.getClass().getSimpleName());

        // CsvToBeanBuilderでBeanにマッピング
        CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                .withMappingStrategy(strategy)
                .withIgnoreLeadingWhiteSpace(true)
                .withIgnoreEmptyLine(true)
                .build();
        log.debug("CsvToBeanBuilder設定完了");

        List<T> result = csvToBean.parse();
        log.debug("Beanマッピング完了: マッピング件数={}", result.size());
        return result;
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
        log.info("カスタム戦略でCSVファイルからBean読み込み開始: ファイルパス={}, Beanクラス={}, 戦略={}", 
                filePath, beanClass.getSimpleName(), strategy.getClass().getSimpleName());
        try (FileReader reader = new FileReader(filePath)) {
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                    .withMappingStrategy(strategy)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();

            List<T> result = csvToBean.parse();
            log.info("カスタム戦略でCSVファイルからBean読み込み完了: 読み込み件数={}", result.size());
            return result;
        } catch (IOException e) {
            log.error("カスタム戦略でCSVファイルからBean読み込み中にIOエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw e;
        }
    }
}
