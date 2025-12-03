package com.example.csv.writer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import com.example.common.config.CharsetType;
import com.example.common.config.FileType;
import com.example.common.config.LineSeparatorType;
import com.example.common.config.QuoteStrategy;
import com.example.exception.CsvWriteException;

import lombok.extern.slf4j.Slf4j;

/**
 * CSV書き込みのラッパークラス
 *
 * <p>このクラスはBeanのリストをCSVファイルに書き込みます。
 * Builderパターンを使用して、柔軟な設定が可能です。</p>
 */
@Slf4j
public class CsvWriterWrapper {

    private Path filePath;
    private Class<?> beanClass;
    private CharsetType charsetType = CharsetType.UTF_8;
    private FileType fileType = FileType.CSV;
    private LineSeparatorType lineSeparatorType = LineSeparatorType.CRLF;
    private boolean usePositionMapping = false;
    private boolean writeHeader = true;
    private QuoteStrategy quoteStrategy = QuoteStrategy.MINIMAL;

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
     * CSVファイルにBeanのListを書き込む（従来のAPI - 互換性維持）
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
            Function<CsvWriterWrapper, Void> writerFunction) throws IOException {
        CsvWriterWrapper wrapper = new CsvWriterWrapper(beanClass, filePath);
        return writerFunction.apply(wrapper);
    }

    /**
     * CSVファイルに書き込む
     *
     * @param <T> Beanの型
     * @param beans 書き込むBeanのList
     * @return Void
     * @throws CsvWriteException CSV書き込みエラー
     */
    @SuppressWarnings("unchecked")
    public <T> Void write(List<T> beans) {
        try {
            CsvStreamWriter.Builder<T> builder = CsvStreamWriter.builder((Class<T>) beanClass, filePath)
                    .charset(charsetType)
                    .fileType(fileType)
                    .lineSeparator(lineSeparatorType)
                    .quoteStrategy(quoteStrategy);

            if (usePositionMapping) {
                builder.usePositionMapping();
            } else {
                builder.useHeaderMapping();
            }

            if (!writeHeader) {
                builder.noHeader();
            }

            builder.write(beans.stream());

            log.info("CSVファイルへの書き込み完了: ファイルパス={}, 件数={}", filePath, beans.size());
            return null;
        } catch (IOException e) {
            log.error("CSVファイル書き込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new CsvWriteException("CSVファイルの書き込みに失敗しました: " + filePath, e);
        }
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
        this.charsetType = charsetType;
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
        this.lineSeparatorType = lineSeparatorType;
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
            wrapper.charsetType = charsetType;
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
         * ヘッダーを出力しない
         *
         * @return このBuilderインスタンス
         */
        public Builder noHeader() {
            wrapper.writeHeader = false;
            return this;
        }

        /**
         * クオート戦略を設定
         *
         * @param quoteStrategy クオート戦略
         * @return このBuilderインスタンス
         */
        public Builder quoteStrategy(QuoteStrategy quoteStrategy) {
            wrapper.quoteStrategy = quoteStrategy;
            return this;
        }

        /**
         * 改行コードを設定
         *
         * @param lineSeparatorType 改行コードタイプ
         * @return このBuilderインスタンス
         */
        public Builder lineSeparator(LineSeparatorType lineSeparatorType) {
            wrapper.lineSeparatorType = lineSeparatorType;
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
