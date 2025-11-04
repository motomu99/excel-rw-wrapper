package com.example.csv.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.opencsv.exceptions.CsvException;
import com.example.common.config.CharsetType;
import com.example.common.config.FileType;
import com.example.common.mapping.MappingStrategyFactory;
import com.example.common.util.BomHandler;
import com.example.exception.CsvReadException;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.MappingStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * CSVファイルをStreamとして読み込むビルダークラス
 * Builderパターンを使用してCSV読み込み処理を抽象化
 * 
 * 使用例:
 * <pre>
 * List<Person> persons = CsvStreamReader.builder(Person.class, Paths.get("sample.csv"))
 *     .charset(CharsetType.UTF_8)
 *     .fileType(FileType.CSV)
 *     .skip(1)
 *     .process(stream -> stream.collect(Collectors.toList()));
 * </pre>
 */
@Slf4j
public class CsvStreamReader<T> {
    
    private final Class<T> beanClass;
    private final Path filePath;
    CharsetType charsetType = CharsetType.UTF_8;
    FileType fileType = FileType.CSV;
    int skipLines = 0;
    boolean usePositionMapping = false;
    
    private CsvStreamReader(Class<T> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }
    
    /**
     * Builderインスタンスを生成
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング先のBeanクラス
     * @param filePath CSVファイルのパス
     * @return Builderインスタンス
     */
    public static <T> Builder<T> builder(Class<T> beanClass, Path filePath) {
        return new Builder<>(beanClass, filePath);
    }
    
    /**
     * Streamを処理する
     * 
     * @param <R> 戻り値の型
     * @param processor Streamを処理する関数
     * @return 処理結果
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    private <R> R process(Function<Stream<T>, R> processor) throws IOException, CsvException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             InputStream is = charsetType.isWithBom() ? BomHandler.skipBom(fis) : fis;
             InputStreamReader isr = new InputStreamReader(is, Charset.forName(charsetType.getCharsetName()))) {
            
            MappingStrategy<T> strategy = MappingStrategyFactory.createStrategy(beanClass, usePositionMapping);
            
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(isr)
                    .withMappingStrategy(strategy)
                    .withSeparator(fileType.getDelimiter().charAt(0))
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();
            
            Stream<T> stream = csvToBean.stream();
            
            // データ行をスキップする処理
            if (skipLines > 0) {
                stream = stream.skip(skipLines);
            }
            
            // 呼び出し側でStreamを処理（try-with-resources内で完了）
            return processor.apply(stream);
        } catch (IOException e) {
            log.error("CSVファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new CsvReadException("CSVファイルの読み込みに失敗しました: " + filePath, e);
        }
    }

    /**
     * 戻り値不要の処理用ショートカット
     * 
     * @param consumer Streamを消費する処理
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     */
    private void process(Consumer<Stream<T>> consumer) throws IOException, CsvException {
        process(stream -> {
            consumer.accept(stream);
            return null;
        });
    }
    
    /**
     * CsvStreamReaderのBuilderクラス
     * 
     * <p>Builderパターンを使用して、CsvStreamReaderの設定を行います。
     * メソッドチェーンで設定を積み重ね、最後に{@link #process(Function)}を呼び出すことで
     * CSVファイルを読み込みます。</p>
     */
    public static class Builder<T> {
        private final CsvStreamReader<T> reader;
        
        private Builder(Class<T> beanClass, Path filePath) {
            this.reader = new CsvStreamReader<>(beanClass, filePath);
        }
        
        /**
         * 文字エンコーディングを設定
         * 
         * @param charsetType 文字エンコーディングタイプ
         * @return このBuilderインスタンス
         */
        public Builder<T> charset(CharsetType charsetType) {
            reader.charsetType = charsetType;
            return this;
        }
        
        /**
         * ファイルタイプを設定
         * 
         * @param fileType ファイルタイプ
         * @return このBuilderインスタンス
         */
        public Builder<T> fileType(FileType fileType) {
            reader.fileType = fileType;
            return this;
        }
        
        /**
         * スキップする行数を設定
         * 
         * @param lines スキップする行数
         * @return このBuilderインスタンス
         */
        public Builder<T> skip(int lines) {
            reader.skipLines = lines;
            return this;
        }
        
        /**
         * 位置ベースマッピングを使用
         * 
         * @return このBuilderインスタンス
         */
        public Builder<T> usePositionMapping() {
            reader.usePositionMapping = true;
            return this;
        }
        
        /**
         * ヘッダーベースマッピングを使用（デフォルト）
         * 
         * @return このBuilderインスタンス
         */
        public Builder<T> useHeaderMapping() {
            reader.usePositionMapping = false;
            return this;
        }
        
        /**
         * Streamを処理する
         * 
         * @param <R> 戻り値の型
         * @param processor Streamを処理する関数
         * @return 処理結果
         * @throws IOException ファイル読み込みエラー
         * @throws CsvException CSV解析エラー
         */
        public <R> R process(Function<Stream<T>, R> processor) throws IOException, CsvException {
            return reader.process(processor);
        }
        
        /**
         * 戻り値不要の処理用ショートカット
         * 
         * @param consumer Streamを消費する処理
         * @throws IOException ファイル読み込みエラー
         * @throws CsvException CSV解析エラー
         */
        public void process(Consumer<Stream<T>> consumer) throws IOException, CsvException {
            reader.process(consumer);
        }
    }
}

