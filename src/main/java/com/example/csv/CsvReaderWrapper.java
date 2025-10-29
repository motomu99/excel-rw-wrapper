package com.example.csv;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.opencsv.exceptions.CsvException;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.MappingStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * CSV読み込みのラッパークラス
 * 
 * <p>このクラスはCSVファイルを読み込み、指定されたBeanクラスにマッピングします。
 * Builderパターンを使用して、柔軟な設定が可能です。</p>
 * 
 * <h3>使用例（新しいBuilderパターン - 推奨）:</h3>
 * <pre>
 * // 基本的な使い方
 * List&lt;Person&gt; persons = CsvReaderWrapper.builder(Person.class, Paths.get("sample.csv"))
 *     .read();
 * 
 * // 詳細設定
 * List&lt;Person&gt; persons = CsvReaderWrapper.builder(Person.class, Paths.get("sample.csv"))
 *     .charset(CharsetType.UTF_8_BOM)
 *     .skipLines(1)
 *     .fileType(FileType.TSV)
 *     .read();
 * </pre>
 * 
 * <h3>使用例（従来のexecuteメソッド - 互換性維持）:</h3>
 * <pre>
 * List&lt;Person&gt; persons = CsvReaderWrapper.execute(Person.class, 
 *     Paths.get("sample.csv"), CsvReaderWrapper::read);
 * </pre>
 */
@Slf4j
public class CsvReaderWrapper {
    
    private int skipLines = 0;
    private Path filePath;
    private Class<?> beanClass;
    private Charset charset = StandardCharsets.UTF_8;
    private FileType fileType = FileType.CSV;
    private boolean usePositionMapping = false;
    private boolean withBom = false;

    private CsvReaderWrapper(Class<?> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }

    /**
     * Builderインスタンスを生成（推奨される使い方）
     * 
     * @param beanClass マッピング先のBeanクラス
     * @param filePath CSVファイルのパス
     * @return Builderインスタンス
     */
    public static Builder builder(Class<?> beanClass, Path filePath) {
        return new Builder(beanClass, filePath);
    }

    /**
     * CSVファイルを読み込んでBeanのListとして返す（従来の使い方 - 互換性維持）
     * 
     * <p>既存コードとの互換性のために残されています。
     * 新しいコードでは {@link #builder(Class, Path)} の使用を推奨します。</p>
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング先のBeanクラス
     * @param filePath CSVファイルのパス
     * @param readerFunction 読み込み処理を行う関数
     * @return BeanのList
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     * @deprecated 新しい {@link #builder(Class, Path)} メソッドを使用してください
     */
    @Deprecated(since = "2.0.0")
    public static <T> List<T> execute(Class<T> beanClass, Path filePath, 
                                     Function<CsvReaderWrapper, List<T>> readerFunction) 
                                     throws IOException, CsvException {
        CsvReaderWrapper wrapper = new CsvReaderWrapper(beanClass, filePath);
        return readerFunction.apply(wrapper);
    }

    /**
     * CSVファイルを読み込む
     * 
     * <p>設定されたパラメータに基づいてCSVファイルを読み込み、
     * Beanのリストとして返します。</p>
     * 
     * @param <T> Beanの型
     * @return BeanのList
     * @throws CsvReadException CSV読み込みエラー
     */
    public <T> List<T> read() {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             InputStream is = withBom ? BomHandler.skipBom(fis) : fis;
             InputStreamReader isr = new InputStreamReader(is, charset)) {
            
            MappingStrategy<T> strategy = createMappingStrategy();
            
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(isr)
                    .withMappingStrategy(strategy)
                    .withSeparator(fileType.getDelimiter().charAt(0))
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();
            
            List<T> result = csvToBean.parse();
            
            return applySkipLines(result);
        } catch (IOException e) {
            log.error("CSVファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new CsvReadException("CSVファイルの読み込みに失敗しました: " + filePath, e);
        }
    }

    /**
     * マッピング戦略を生成
     * 
     * @param <T> Beanの型
     * @return マッピング戦略
     */
    @SuppressWarnings("unchecked")
    private <T> MappingStrategy<T> createMappingStrategy() {
        return MappingStrategyFactory.createStrategy((Class<T>) this.beanClass, usePositionMapping);
    }

    /**
     * スキップ行数を適用
     * 
     * @param <T> Beanの型
     * @param result 元のリスト
     * @return スキップ処理後のリスト
     */
    private <T> List<T> applySkipLines(List<T> result) {
        if (skipLines <= 0) {
            return result;
        }
        if (skipLines >= result.size()) {
            return new ArrayList<>();
        }
        return result.subList(skipLines, result.size());
    }

    /**
     * スキップ行数を設定（従来のAPI - 互換性維持）
     * 
     * @param skipLines スキップする行数
     * @return このインスタンス
     * @deprecated {@link Builder#skipLines(int)} を使用してください
     */
    @Deprecated(since = "2.0.0")
    public CsvReaderWrapper setSkip(int skipLines) {
        this.skipLines = skipLines;
        return this;
    }

    /**
     * 文字セットを設定（従来のAPI - 互換性維持）
     * 
     * @param charsetType 文字セットタイプ
     * @return このインスタンス
     * @deprecated {@link Builder#charset(CharsetType)} を使用してください
     */
    @Deprecated(since = "2.0.0")
    public CsvReaderWrapper setCharset(CharsetType charsetType) {
        this.charset = Charset.forName(charsetType.getCharsetName());
        this.withBom = charsetType.isWithBom();
        return this;
    }

    /**
     * ファイルタイプを設定（従来のAPI - 互換性維持）
     * 
     * @param fileType ファイルタイプ
     * @return このインスタンス
     * @deprecated {@link Builder#fileType(FileType)} を使用してください
     */
    @Deprecated(since = "2.0.0")
    public CsvReaderWrapper setFileType(FileType fileType) {
        this.fileType = fileType;
        return this;
    }

    /**
     * 位置ベースマッピングを使用（従来のAPI - 互換性維持）
     * 
     * @return このインスタンス
     */
    public CsvReaderWrapper usePositionMapping() {
        this.usePositionMapping = true;
        return this;
    }

    /**
     * ヘッダーベースマッピングを使用（従来のAPI - 互換性維持）
     * 
     * @return このインスタンス
     */
    public CsvReaderWrapper useHeaderMapping() {
        this.usePositionMapping = false;
        return this;
    }

    /**
     * CsvReaderWrapperのBuilderクラス
     * 
     * <p>Builderパターンを使用して、CsvReaderWrapperの設定を行います。
     * メソッドチェーンで設定を積み重ね、最後に{@link #read()}を呼び出すことで
     * CSVファイルを読み込みます。</p>
     */
    public static class Builder {
        private final CsvReaderWrapper wrapper;
        
        private Builder(Class<?> beanClass, Path filePath) {
            this.wrapper = new CsvReaderWrapper(beanClass, filePath);
        }
        
        /**
         * スキップする行数を設定
         * 
         * @param skipLines スキップする行数（データ行のみ、ヘッダーは含まない）
         * @return このBuilderインスタンス
         */
        public Builder skipLines(int skipLines) {
            wrapper.skipLines = skipLines;
            return this;
        }
        
        /**
         * 文字セットを設定
         * 
         * @param charsetType 文字セットタイプ
         * @return このBuilderインスタンス
         */
        public Builder charset(CharsetType charsetType) {
            wrapper.charset = Charset.forName(charsetType.getCharsetName());
            wrapper.withBom = charsetType.isWithBom();
            return this;
        }
        
        /**
         * ファイルタイプを設定
         * 
         * @param fileType ファイルタイプ（CSVまたはTSV）
         * @return このBuilderインスタンス
         */
        public Builder fileType(FileType fileType) {
            wrapper.fileType = fileType;
            return this;
        }
        
        /**
         * 位置ベースマッピングを使用
         * 
         * <p>ヘッダー行がない場合に使用します。
         * カラムの位置に基づいてBeanのフィールドにマッピングします。</p>
         * 
         * @return このBuilderインスタンス
         */
        public Builder usePositionMapping() {
            wrapper.usePositionMapping = true;
            return this;
        }
        
        /**
         * ヘッダーベースマッピングを使用（デフォルト）
         * 
         * <p>ヘッダー行の名前に基づいてBeanのフィールドにマッピングします。</p>
         * 
         * @return このBuilderインスタンス
         */
        public Builder useHeaderMapping() {
            wrapper.usePositionMapping = false;
            return this;
        }
        
        /**
         * CSVファイルを読み込む
         * 
         * @param <T> Beanの型
         * @return BeanのList
         * @throws CsvReadException CSV読み込みエラー
         */
        public <T> List<T> read() {
            return wrapper.read();
        }
    }
}
