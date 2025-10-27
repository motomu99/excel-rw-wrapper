package com.example.csv;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.nio.charset.Charset;

import com.opencsv.exceptions.CsvException;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class CsvReaderWrapper {
    
    private int skipLines = 0;
    private Path filePath;
    private Class<?> beanClass;
    private Charset charset = Charset.forName("UTF-8");
    private FileType fileType = FileType.CSV;
    private boolean usePositionMapping = false;

    private CsvReaderWrapper(Class<?> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }

    /**
     * CSVファイルを読み込んでBeanのListとして返す（CsvReaderWrapperインスタンス用）
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
                                     Function<CsvReaderWrapper, List<T>> readerFunction) 
                                     throws IOException, CsvException {
            CsvReaderWrapper wrapper = new CsvReaderWrapper(beanClass, filePath);
        return readerFunction.apply(wrapper);
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
    @SuppressWarnings("unchecked")
    public <T> List<T> read() {
        try {
            Object strategy;
            if (usePositionMapping) {
                ColumnPositionMappingStrategy<T> positionStrategy = new ColumnPositionMappingStrategy<>();
                positionStrategy.setType((Class<? extends T>) this.beanClass);
                strategy = positionStrategy;
            } else {
                HeaderColumnNameMappingStrategy<T> headerStrategy = new HeaderColumnNameMappingStrategy<>();
                headerStrategy.setType((Class<? extends T>) this.beanClass);
                strategy = headerStrategy;
            }
            
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(new java.io.InputStreamReader(
                    new java.io.FileInputStream(filePath.toFile()), charset))
                    .withMappingStrategy(strategy)
                    .withSeparator(fileType.getDelimiter().charAt(0))
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();
            
            List<T> result = csvToBean.parse();
            
            // データ行をスキップする処理
            if (this.skipLines > 0 && this.skipLines < result.size()) {
                return result.subList(this.skipLines, result.size());
            } else if (this.skipLines >= result.size()) {
                return new java.util.ArrayList<>();
            } else {
                return result;
            }
        } catch (IOException e) {
            log.error("CSVファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public CsvReaderWrapper setSkip(int skipLines) {
        this.skipLines = skipLines;
        return this;
    }

    public CsvReaderWrapper setCharset(CharsetType charsetType) {
        this.charset = Charset.forName(charsetType.getCharsetName());
        return this;
    }

    public CsvReaderWrapper setFileType(FileType fileType) {
        this.fileType = fileType;
        return this;
    }

    public CsvReaderWrapper usePositionMapping(boolean usePositionMapping) {
        this.usePositionMapping = usePositionMapping;
        return this;
    }

    public CsvReaderWrapper usePositionMapping() {
        this.usePositionMapping = true;
        return this;
    }

    public CsvReaderWrapper useHeaderMapping() {
        this.usePositionMapping = false;
        return this;
    }
}
