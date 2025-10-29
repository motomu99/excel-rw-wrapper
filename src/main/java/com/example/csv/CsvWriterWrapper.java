package com.example.csv;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.bean.MappingStrategy;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.extern.slf4j.Slf4j;

/**
 * CSV書き込みのラッパークラス
 * 
 * <p>このクラスはBeanのリストをCSVファイルに書き込みます。
 * Builderパターンを使用して、柔軟な設定が可能です。</p>
 * 
 * <h3>使用例（新しいBuilderパターン - 推奨）:</h3>
 * <pre>
 * // 基本的な使い方
 * List&lt;Person&gt; persons = Arrays.asList(new Person("田中", 25));
 * CsvWriterWrapper.builder(Person.class, Paths.get("output.csv"))
 *     .write(persons);
 * 
 * // 詳細設定
 * CsvWriterWrapper.builder(Person.class, Paths.get("output.csv"))
 *     .charset(CharsetType.UTF_8_BOM)
 *     .fileType(FileType.TSV)
 *     .lineSeparator(LineSeparatorType.LF)
 *     .write(persons);
 * </pre>
 * 
 * <h3>使用例（従来のexecuteメソッド - 互換性維持）:</h3>
 * <pre>
 * List&lt;Person&gt; persons = Arrays.asList(new Person("田中", 25));
 * CsvWriterWrapper.execute(Person.class, 
 *     Paths.get("output.csv"), 
 *     instance -> instance.write(persons));
 * </pre>
 */
@Slf4j
public class CsvWriterWrapper {
    
    private Path filePath;
    private Class<?> beanClass;
    private Charset charset = StandardCharsets.UTF_8;
    private FileType fileType = FileType.CSV;
    private boolean usePositionMapping = false;
    private boolean withBom = false;
    private String lineSeparator = LineSeparatorType.CRLF.getSeparator();

    private CsvWriterWrapper(Class<?> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }

    /**
     * Builderインスタンスを生成（推奨される使い方）
     * 
     * @param beanClass マッピング元のBeanクラス
     * @param filePath CSVファイルのパス
     * @return Builderインスタンス
     */
    public static Builder builder(Class<?> beanClass, Path filePath) {
        return new Builder(beanClass, filePath);
    }

    /**
     * CSVファイルにBeanのListを書き込む（従来の使い方 - 互換性維持）
     * 
     * <p>既存コードとの互換性のために残されています。
     * 新しいコードでは {@link #builder(Class, Path)} の使用を推奨します。</p>
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング元のBeanクラス
     * @param filePath CSVファイルのパス
     * @param writerFunction 書き込み処理を行う関数
     * @return 処理結果（Void）
     * @throws IOException ファイル書き込みエラー
     * @deprecated 新しい {@link #builder(Class, Path)} メソッドを使用してください
     */
    @Deprecated(since = "2.0.0")
    public static <T> Void execute(Class<T> beanClass, Path filePath, 
                                   Function<CsvWriterWrapper, Void> writerFunction) 
                                   throws IOException {
        CsvWriterWrapper wrapper = new CsvWriterWrapper(beanClass, filePath);
        return writerFunction.apply(wrapper);
    }

    /**
     * CSVファイルに書き込む
     * 
     * <p>設定されたパラメータに基づいてBeanのリストをCSVファイルに書き込みます。</p>
     * 
     * @param <T> Beanの型
     * @param beans 書き込むBeanのList
     * @return Void
     * @throws CsvWriteException CSV書き込みエラー
     */
    public <T> Void write(List<T> beans) {
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            
            // BOM付きUTF-8の場合、BOMを書き込む
            if (withBom) {
                BomHandler.writeBom(fos);
            }
            
            try (OutputStreamWriter osw = new OutputStreamWriter(fos, charset)) {
                MappingStrategy<T> strategy = createMappingStrategy();
                
                StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(osw)
                        .withMappingStrategy(strategy)
                        .withSeparator(fileType.getDelimiter().charAt(0))
                        .withLineEnd(lineSeparator)
                        .build();
                
                beanToCsv.write(beans);
                
                log.info("CSVファイルへの書き込み完了: ファイルパス={}, 件数={}", filePath, beans.size());
            }
            
            return null;
        } catch (IOException e) {
            log.error("CSVファイル書き込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new CsvWriteException("CSVファイルの書き込みに失敗しました: " + filePath, e);
        } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            log.error("CSV書き込み中にデータ型エラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new CsvWriteException("CSVデータの変換に失敗しました: " + filePath, e);
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
     * 文字エンコーディングを設定（従来のAPI - 互換性維持）
     * 
     * @param charsetType 文字エンコーディング
     * @return このインスタンス（メソッドチェーン用）
     * @deprecated {@link Builder#charset(CharsetType)} を使用してください
     */
    @Deprecated(since = "2.0.0")
    public CsvWriterWrapper setCharset(CharsetType charsetType) {
        this.charset = Charset.forName(charsetType.getCharsetName());
        this.withBom = charsetType.isWithBom();
        return this;
    }

    /**
     * ファイルタイプ（CSV/TSV）を設定（従来のAPI - 互換性維持）
     * 
     * @param fileType ファイルタイプ
     * @return このインスタンス（メソッドチェーン用）
     * @deprecated {@link Builder#fileType(FileType)} を使用してください
     */
    @Deprecated(since = "2.0.0")
    public CsvWriterWrapper setFileType(FileType fileType) {
        this.fileType = fileType;
        return this;
    }

    /**
     * ポジションマッピングを使用（ヘッダーなし）
     * 
     * @return このインスタンス（メソッドチェーン用）
     */
    public CsvWriterWrapper usePositionMapping() {
        this.usePositionMapping = true;
        return this;
    }

    /**
     * ヘッダーマッピングを使用（ヘッダーあり）
     * 
     * @return このインスタンス（メソッドチェーン用）
     */
    public CsvWriterWrapper useHeaderMapping() {
        this.usePositionMapping = false;
        return this;
    }

    /**
     * 改行コードを設定（従来のAPI - 互換性維持）
     * 
     * @param lineSeparatorType 改行コードタイプ
     * @return このインスタンス（メソッドチェーン用）
     * @deprecated {@link Builder#lineSeparator(LineSeparatorType)} を使用してください
     */
    @Deprecated(since = "2.0.0")
    public CsvWriterWrapper setLineSeparator(LineSeparatorType lineSeparatorType) {
        this.lineSeparator = lineSeparatorType.getSeparator();
        return this;
    }

    /**
     * CsvWriterWrapperのBuilderクラス
     * 
     * <p>Builderパターンを使用して、CsvWriterWrapperの設定を行います。
     * メソッドチェーンで設定を積み重ね、最後に{@link #write(List)}を呼び出すことで
     * CSVファイルに書き込みます。</p>
     */
    public static class Builder {
        private final CsvWriterWrapper wrapper;
        
        private Builder(Class<?> beanClass, Path filePath) {
            this.wrapper = new CsvWriterWrapper(beanClass, filePath);
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
         * <p>ヘッダー行を出力しない場合に使用します。
         * カラムの位置に基づいてBeanのフィールドを出力します。</p>
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
         * <p>ヘッダー行を含めてCSVファイルを出力します。</p>
         * 
         * @return このBuilderインスタンス
         */
        public Builder useHeaderMapping() {
            wrapper.usePositionMapping = false;
            return this;
        }
        
        /**
         * 改行コードを設定
         * 
         * @param lineSeparatorType 改行コードタイプ
         * @return このBuilderインスタンス
         */
        public Builder lineSeparator(LineSeparatorType lineSeparatorType) {
            wrapper.lineSeparator = lineSeparatorType.getSeparator();
            return this;
        }
        
        /**
         * CSVファイルに書き込む
         * 
         * @param <T> Beanの型
         * @param beans 書き込むBeanのList
         * @return Void
         * @throws CsvWriteException CSV書き込みエラー
         */
        public <T> Void write(List<T> beans) {
            return wrapper.write(beans);
        }
    }
}
