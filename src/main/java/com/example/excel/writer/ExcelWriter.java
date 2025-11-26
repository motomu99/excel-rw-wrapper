package com.example.excel.writer;

import java.nio.file.Path;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Excelファイルを一括で書き込むビルダークラス
 * Builderパターンを使用してExcel書き込み処理を抽象化
 *
 * <p>このクラスはExcelファイルに一括でデータを書き込みます。
 * 大きなファイルの場合は {@link ExcelStreamWriter} を使用した
 * ストリーミング処理を推奨します。</p>
 *
 * 使用例:
 * <pre>
 * // 基本的な使用方法（ヘッダーベースマッピング）
 * ExcelWriter.builder(Person.class, Paths.get("output.xlsx"))
 *     .write(persons);
 *
 * // シート名を指定
 * ExcelWriter.builder(Person.class, Paths.get("output.xlsx"))
 *     .sheetName("社員データ")
 *     .write(persons);
 *
 * // 位置ベースマッピングを使用
 * ExcelWriter.builder(PersonWithoutHeader.class, Paths.get("output.xlsx"))
 *     .usePositionMapping()
 *     .write(persons);
 *
 * // 既存ファイル（テンプレート）にデータを書き込む
 * ExcelWriter.builder(Person.class, Paths.get("template.xlsx"))
 *     .loadExisting()
 *     .sheetName("データ")
 *     .startCell(2, 0)  // A3セルから書き込み開始
 *     .write(persons);
 * </pre>
 */
@Slf4j
public class ExcelWriter<T> {

    private final Class<T> beanClass;
    private final Path filePath;
    String sheetName = "Sheet1";
    int sheetIndex = 0;
    boolean usePositionMapping = false;
    boolean loadExisting = false;
    int startRow = 0;
    int startColumn = 0;

    private ExcelWriter(Class<T> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }

    /**
     * Builderインスタンスを生成
     *
     * @param <T> Beanの型
     * @param beanClass 書き込むBeanクラス
     * @param filePath 出力先のExcelファイルパス
     * @return Builderインスタンス
     */
    public static <T> Builder<T> builder(Class<T> beanClass, Path filePath) {
        return new Builder<>(beanClass, filePath);
    }

    /**
     * Listを書き込む
     * 
     * <p>設定されたパラメータに基づいてExcelファイルにデータを書き込みます。</p>
     * 
     * @param list 書き込むデータのList
     */
    public void write(List<T> list) {
        // ExcelStreamWriterの内部実装を再利用
        ExcelStreamWriter.Builder<T> builder = ExcelStreamWriter.builder(beanClass, filePath)
            .sheetName(sheetName)
            .sheetIndex(sheetIndex)
            .startCell(startRow, startColumn);
        
        if (usePositionMapping) {
            builder.usePositionMapping();
        } else {
            builder.useHeaderMapping();
        }
        
        if (loadExisting) {
            builder.loadExisting();
        }
        
        builder.write(list.stream());
    }

    /**
     * ExcelWriterのBuilderクラス
     *
     * <p>Builderパターンを使用して、ExcelWriterの設定を行います。
     * メソッドチェーンで設定を積み重ね、最後に{@link #write(List)}を呼び出すことで
     * Excelファイルに書き込みます。</p>
     */
    public static class Builder<T> {
        private final ExcelWriter<T> writer;
        
        private Builder(Class<T> beanClass, Path filePath) {
            this.writer = new ExcelWriter<>(beanClass, filePath);
        }
        
        /**
         * シート名を設定
         *
         * @param sheetName シート名
         * @return このBuilderインスタンス
         */
        public Builder<T> sheetName(String sheetName) {
            writer.sheetName = sheetName;
            return this;
        }
        
        /**
         * シートのインデックスを設定（0から始まる）
         *
         * @param sheetIndex シートのインデックス
         * @return このBuilderインスタンス
         */
        public Builder<T> sheetIndex(int sheetIndex) {
            writer.sheetIndex = sheetIndex;
            return this;
        }
        
        /**
         * 位置ベースマッピングを使用
         *
         * @return このBuilderインスタンス
         */
        public Builder<T> usePositionMapping() {
            writer.usePositionMapping = true;
            return this;
        }
        
        /**
         * ヘッダーベースマッピングを使用（デフォルト）
         *
         * @return このBuilderインスタンス
         */
        public Builder<T> useHeaderMapping() {
            writer.usePositionMapping = false;
            return this;
        }
        
        /**
         * 既存ファイルを読み込んで書き込む
         * テンプレートファイルにデータを追記する際に使用
         *
         * @return このBuilderインスタンス
         */
        public Builder<T> loadExisting() {
            writer.loadExisting = true;
            return this;
        }
        
        /**
         * 書き込み開始セルを設定
         * 指定した行・列から書き込みを開始する（0始まり）
         *
         * @param row 開始行（0始まり）
         * @param column 開始列（0始まり）
         * @return このBuilderインスタンス
         */
        public Builder<T> startCell(int row, int column) {
            writer.startRow = row;
            writer.startColumn = column;
            return this;
        }
        
        /**
         * Listを書き込む
         *
         * <p>Listの要素をExcelファイルに書き込みます。
         * 最初の行にはヘッダーが自動的に作成され、それ以降の行にデータが書き込まれます。
         * 日付型（LocalDate, LocalDateTime）は自動的に適切なフォーマットで書き込まれます。</p>
         *
         * @param list 書き込むデータのList
         */
        public void write(List<T> list) {
            writer.write(list);
        }
    }
}

