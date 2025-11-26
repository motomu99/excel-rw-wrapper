package com.example.excel.reader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.exception.HeaderNotFoundException;
import com.example.exception.KeyColumnNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * Excelファイルを一括で読み込むビルダークラス
 * Builderパターンを使用してExcel読み込み処理を抽象化
 *
 * <p>このクラスはExcelファイルを一括で読み込み、Listとして返します。
 * メモリに全てのデータを保持するため、大きなファイルの場合は
 * {@link ExcelStreamReader}を使用したストリーミング処理を推奨します。</p>
 *
 * 使用例:
 * <pre>
 * // 基本的な使用方法
 * List&lt;Person&gt; persons = ExcelReader.builder(Person.class, Paths.get("sample.xlsx"))
 *     .sheetIndex(0)
 *     .skip(1)
 *     .read();
 *
 * // ヘッダー行を自動検出（上から10行以内で「名前」列を探す）
 * List&lt;Person&gt; persons = ExcelReader.builder(Person.class, Paths.get("sample.xlsx"))
 *     .headerKey("名前")
 *     .read();
 *
 * // 複数ファイルを順番に処理
 * List&lt;Person&gt; persons = ExcelReader.builder(Person.class, Arrays.asList(path1, path2))
 *     .read();
 * </pre>
 */
@Slf4j
public class ExcelReader<T> {

    private final Class<T> beanClass;
    private final Path filePath;
    int sheetIndex = 0;
    String sheetName = null;
    int skipLines = 0;
    boolean usePositionMapping = false;
    String headerKeyColumn = null;
    int headerSearchRows = 10; // デフォルトのヘッダー探索行数

    private ExcelReader(Class<T> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }

    /**
     * Builderインスタンスを生成
     *
     * @param <T> Beanの型
     * @param beanClass マッピング先のBeanクラス
     * @param filePath Excelファイルのパス
     * @return Builderインスタンス
     */
    public static <T> Builder<T> builder(Class<T> beanClass, Path filePath) {
        return new Builder<>(beanClass, filePath);
    }

    /**
     * Builderインスタンスを生成（複数ファイル用）
     *
     * @param <T> Beanの型
     * @param beanClass マッピング先のBeanクラス
     * @param filePaths Excelファイルのパスリスト
     * @return Builderインスタンス
     */
    public static <T> Builder<T> builder(Class<T> beanClass, List<Path> filePaths) {
        return new Builder<>(beanClass, filePaths);
    }

    /**
     * Excelファイルを読み込む
     * 
     * <p>設定されたパラメータに基づいてExcelファイルを読み込み、
     * Beanのリストとして返します。</p>
     * 
     * @return BeanのList
     * @throws IOException ファイル読み込みエラー
     * @throws SheetNotFoundException シートが見つからない場合
     * @throws HeaderNotFoundException ヘッダー行が見つからない場合
     * @throws KeyColumnNotFoundException キー列が見つからない場合
     */
    public List<T> read() throws IOException {
        try {
            // ExcelStreamReaderの内部実装を再利用
            ExcelStreamReader.Builder<T> builder = ExcelStreamReader.builder(beanClass, filePath)
                .sheetIndex(sheetIndex)
                .skip(skipLines)
                .headerSearchRows(headerSearchRows);
            
            if (sheetName != null) {
                builder.sheetName(sheetName);
            }
            
            if (usePositionMapping) {
                builder.usePositionMapping();
            } else {
                builder.useHeaderMapping();
            }
            
            if (headerKeyColumn != null) {
                builder.headerKey(headerKeyColumn);
            }
            
            Function<Stream<T>, List<T>> collector = stream -> stream.collect(Collectors.toList());
            return builder.process(collector);
        } catch (com.example.exception.ExcelReaderException e) {
            // ExcelReaderException（SheetNotFoundException, HeaderNotFoundException, KeyColumnNotFoundExceptionなど）
            // はそのまま再スロー（IOExceptionのサブクラスなので、throws IOExceptionで宣言済み）
            throw e;
        }
    }

    /**
     * 現在の設定をコピーし、新しいファイルパスを設定したインスタンスを作成
     * 
     * @param newPath 新しいファイルパス
     * @return 設定がコピーされた新しいインスタンス
     */
    private ExcelReader<T> cloneConfig(Path newPath) {
        ExcelReader<T> clone = new ExcelReader<>(this.beanClass, newPath);
        clone.sheetIndex = this.sheetIndex;
        clone.sheetName = this.sheetName;
        clone.skipLines = this.skipLines;
        clone.usePositionMapping = this.usePositionMapping;
        clone.headerKeyColumn = this.headerKeyColumn;
        clone.headerSearchRows = this.headerSearchRows;
        return clone;
    }

    /**
     * ExcelReaderのBuilderクラス
     *
     * <p>Builderパターンを使用して、ExcelReaderの設定を行います。
     * メソッドチェーンで設定を積み重ね、最後に{@link #read()}を呼び出すことで
     * Excelファイルを読み込みます。</p>
     */
    public static class Builder<T> {
        private final ExcelReader<T> reader;
        private final List<Path> filePaths;
        private int parallelism = 1;
        
        private Builder(Class<T> beanClass, Path filePath) {
            this.reader = new ExcelReader<>(beanClass, filePath);
            this.filePaths = java.util.Collections.singletonList(filePath);
        }
        
        private Builder(Class<T> beanClass, List<Path> filePaths) {
            if (filePaths == null || filePaths.isEmpty()) {
                throw new IllegalArgumentException("filePaths must not be empty or null");
            }
            this.reader = new ExcelReader<>(beanClass, filePaths.get(0));
            this.filePaths = filePaths;
        }
        
        /**
         * シートのインデックスを設定（0から始まる）
         *
         * @param sheetIndex シートのインデックス
         * @return このBuilderインスタンス
         */
        public Builder<T> sheetIndex(int sheetIndex) {
            reader.sheetIndex = sheetIndex;
            reader.sheetName = null;
            return this;
        }
        
        /**
         * シート名を設定
         *
         * @param sheetName シート名
         * @return このBuilderインスタンス
         */
        public Builder<T> sheetName(String sheetName) {
            reader.sheetName = sheetName;
            reader.sheetIndex = 0; // シート名指定時はインデックスをリセット
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
         * ヘッダー行を自動検出するためのキー列名を設定
         * 指定された列名を持つ行を、上から指定行数の範囲内で探してヘッダー行とする
         * また、この列の値が空になったらデータ読み込みを終了する
         *
         * <p>注意: read()メソッド実行時に以下の例外が投げられる可能性があります：
         * <ul>
         *   <li>{@link HeaderNotFoundException} - キー列を持つヘッダー行が見つからない場合</li>
         *   <li>{@link KeyColumnNotFoundException} - ヘッダー行にキー列が存在しない場合</li>
         * </ul>
         *
         * @param keyColumnName キーとなる列名
         * @return このBuilderインスタンス
         */
        public Builder<T> headerKey(String keyColumnName) {
            reader.headerKeyColumn = keyColumnName;
            return this;
        }
        
        /**
         * ヘッダー行を探索する最大行数を設定（デフォルト: 10行）
         * headerKey()と組み合わせて使用する
         *
         * @param rows 探索する最大行数
         * @return このBuilderインスタンス
         */
        public Builder<T> headerSearchRows(int rows) {
            reader.headerSearchRows = rows;
            return this;
        }
        
        /**
         * 並列度を設定
         * 
         * @param parallelism 並列度（1以下の場合は逐次処理）
         * @return このBuilderインスタンス
         */
        public Builder<T> parallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }
        
        /**
         * Excelファイルを読み込む
         * 
         * @return BeanのList
         * @throws IOException ファイル読み込みエラー
         */
        public List<T> read() throws IOException {
            if (filePaths.size() > 1) {
                return readAll();
            }
            return reader.read();
        }

        /**
         * 全ファイルを読み込む
         * 
         * <p>設定されたファイルリストを読み込み、
         * 並列度に従って処理を実行します。</p>
         * 
         * @return BeanのList
         * @throws IOException ファイル読み込みエラー
         */
        public List<T> readAll() throws IOException {
            try {
                return com.example.common.reader.ParallelReadExecutor.readAll(
                    filePaths,
                    path -> {
                        try {
                            return reader.cloneConfig(path).read();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    },
                    parallelism
                );
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) e.getCause();
                }
                throw e;
            }
        }
    }
}

