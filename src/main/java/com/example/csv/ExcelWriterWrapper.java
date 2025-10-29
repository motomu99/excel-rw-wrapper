package com.example.csv;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

/**
 * Excel書き込みのラッパークラス
 * 
 * <p>このクラスはBeanのリストをExcelファイルに書き込みます。
 * Builderパターンを使用して、柔軟な設定が可能です。</p>
 * 
 * <h3>使用例（新しいBuilderパターン - 推奨）:</h3>
 * <pre>
 * // 基本的な使い方
 * List&lt;Person&gt; persons = Arrays.asList(new Person("田中", 25));
 * ExcelWriterWrapper.builder(Person.class, Paths.get("output.xlsx"))
 *     .write(persons);
 * 
 * // 詳細設定
 * ExcelWriterWrapper.builder(Person.class, Paths.get("output.xlsx"))
 *     .sheetName("社員データ")
 *     .useHeaderMapping()
 *     .write(persons);
 * </pre>
 * 
 * <h3>使用例（従来のexecuteメソッド - 互換性維持）:</h3>
 * <pre>
 * List&lt;Person&gt; persons = Arrays.asList(new Person("田中", 25));
 * ExcelWriterWrapper.execute(Person.class, 
 *     Paths.get("output.xlsx"), 
 *     instance -> instance.write(persons));
 * </pre>
 */
@Slf4j
public class ExcelWriterWrapper {
    
    private Path filePath;
    private Class<?> beanClass;
    private String sheetName = "Sheet1";
    private boolean usePositionMapping = false;

    private ExcelWriterWrapper(Class<?> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }

    /**
     * Builderインスタンスを生成（推奨される使い方）
     * 
     * @param beanClass マッピング元のBeanクラス
     * @param filePath Excelファイルのパス
     * @return Builderインスタンス
     */
    public static Builder builder(Class<?> beanClass, Path filePath) {
        return new Builder(beanClass, filePath);
    }

    /**
     * ExcelファイルにBeanのListを書き込む（従来の使い方 - 互換性維持）
     * 
     * <p>既存コードとの互換性のために残されています。
     * 新しいコードでは {@link #builder(Class, Path)} の使用を推奨します。</p>
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング元のBeanクラス
     * @param filePath Excelファイルのパス
     * @param writerFunction 書き込み処理を行う関数
     * @return 処理結果（Void）
     * @throws IOException ファイル書き込みエラー
     * @deprecated 新しい {@link #builder(Class, Path)} メソッドを使用してください
     */
    @Deprecated(since = "2.0.0")
    public static <T> Void execute(Class<T> beanClass, Path filePath, 
                                   Function<ExcelWriterWrapper, Void> writerFunction) 
                                   throws IOException {
        ExcelWriterWrapper wrapper = new ExcelWriterWrapper(beanClass, filePath);
        return writerFunction.apply(wrapper);
    }

    /**
     * Excelファイルに書き込む
     * 
     * <p>設定されたパラメータに基づいてBeanのリストをExcelファイルに書き込みます。</p>
     * 
     * @param <T> Beanの型
     * @param beans 書き込むBeanのList
     * @return Void
     * @throws RuntimeException Excel書き込みエラー（IOException等をラップ）
     */
    public <T> Void write(List<T> beans) {
        try {
            ExcelStreamWriter<T> writer = ExcelStreamWriter.of((Class<T>) this.beanClass, this.filePath);
            
            // シート名を設定
            writer.sheetName(this.sheetName);
            
            // マッピング方式を設定
            if (this.usePositionMapping) {
                writer.usePositionMapping();
            } else {
                writer.useHeaderMapping();
            }
            
            // 書き込み実行
            writer.write(beans.stream());
            
            log.info("Excelファイルへの書き込み完了: ファイルパス={}, 件数={}", filePath, beans.size());
            
            return null;
        } catch (IOException e) {
            log.error("Excelファイル書き込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new RuntimeException("Excelファイルの書き込みに失敗しました: " + filePath, e);
        }
    }

    /**
     * シート名を設定
     * 
     * @param sheetName シート名
     * @return このインスタンス（メソッドチェーン用）
     */
    public ExcelWriterWrapper setSheetName(String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    /**
     * ポジションマッピングを使用（ヘッダーなし）
     * 
     * @return このインスタンス（メソッドチェーン用）
     */
    public ExcelWriterWrapper usePositionMapping() {
        this.usePositionMapping = true;
        return this;
    }

    /**
     * ヘッダーマッピングを使用（ヘッダーあり）
     * 
     * @return このインスタンス（メソッドチェーン用）
     */
    public ExcelWriterWrapper useHeaderMapping() {
        this.usePositionMapping = false;
        return this;
    }

    /**
     * ExcelWriterWrapperのBuilderクラス
     * 
     * <p>Builderパターンを使用して、ExcelWriterWrapperの設定を行います。
     * メソッドチェーンで設定を積み重ね、最後に{@link #write(List)}を呼び出すことで
     * Excelファイルに書き込みます。</p>
     */
    public static class Builder {
        private final ExcelWriterWrapper wrapper;
        
        private Builder(Class<?> beanClass, Path filePath) {
            this.wrapper = new ExcelWriterWrapper(beanClass, filePath);
        }
        
        /**
         * シート名を設定
         * 
         * @param sheetName シート名
         * @return このBuilderインスタンス
         */
        public Builder sheetName(String sheetName) {
            wrapper.sheetName = sheetName;
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
         * <p>ヘッダー行を含めてExcelファイルを出力します。</p>
         * 
         * @return このBuilderインスタンス
         */
        public Builder useHeaderMapping() {
            wrapper.usePositionMapping = false;
            return this;
        }
        
        /**
         * Excelファイルに書き込む
         * 
         * @param <T> Beanの型
         * @param beans 書き込むBeanのList
         * @return Void
         * @throws RuntimeException Excel書き込みエラー
         */
        public <T> Void write(List<T> beans) {
            return wrapper.write(beans);
        }
    }
}
