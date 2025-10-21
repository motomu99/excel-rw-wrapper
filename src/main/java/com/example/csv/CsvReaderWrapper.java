package com.example.csv;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import com.opencsv.exceptions.CsvException;

/**
 * CSV読み込みのラッパークラス
 * 関数型インターフェースを使用してCSV読み込み処理を抽象化
 * 
 * 使用例:
 * <pre>
 * List<Person> persons = CsvReaderWrapper.execute(Person.class, 
 *     Paths.get("sample.csv"), CsvReaderWrapper::read);
 * </pre>
 */
public class CsvReaderWrapper {

    /**
     * CSVファイルを読み込んでBeanのListとして返す
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング先のBeanクラス
     * @param filePath CSVファイルのパス
     * @param readerFunction 読み込み処理を行う関数
     * @return BeanのList
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    public static <T> List<T> execute(Class<T> beanClass, Path filePath, 
                                     BiFunction<Class<T>, Path, List<T>> readerFunction) 
                                     throws IOException, CsvException {
        try {
            return readerFunction.apply(beanClass, filePath);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            } else if (e.getCause() instanceof CsvException) {
                throw (CsvException) e.getCause();
            }
            throw e;
        }
    }

    /**
     * CSVファイルを読み込む関数
     * 標準のCsvBeanReaderを使用したマッピング
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング先のBeanクラス
     * @param filePath CSVファイルのパス
     * @return BeanのList
     */
    public static <T> List<T> read(Class<T> beanClass, Path filePath) {
        try {
            // 標準のCsvBeanReaderを使用してCSVファイルを読み込み
            CsvBeanReader reader = new CsvBeanReader();
            return reader.readCsvToBeans(filePath.toString(), beanClass);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
