package com.example.excel.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.example.exception.HeaderNotFoundException;
import com.example.exception.KeyColumnNotFoundException;
import com.example.exception.SheetNotFoundException;
import com.example.exception.UncheckedExcelException;
import com.github.pjfanning.xlsx.StreamingReader;

import lombok.extern.slf4j.Slf4j;

/**
 * ExcelファイルをStreamとして読み込むビルダークラス
 * Builderパターンを使用してExcel読み込み処理を抽象化
 *
 * 使用例:
 * <pre>
 * // 基本的な使用方法
 * List&lt;Person&gt; persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
 *     .sheetIndex(0)
 *     .skip(1)
 *     .process(stream -&gt; stream.collect(Collectors.toList()));
 *
 * // ヘッダー行を自動検出（上から10行以内で「名前」列を探す）
 * List&lt;Person&gt; persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
 *     .headerKey("名前")
 *     .process(stream -&gt; stream.collect(Collectors.toList()));
 *
 * // ヘッダー行の探索範囲を20行に拡張
 * List&lt;Person&gt; persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
 *     .headerKey("名前")
 *     .headerSearchRows(20)
 *     .process(stream -&gt; stream.collect(Collectors.toList()));
 * </pre>
 */
@Slf4j
public class ExcelStreamReader<T> {

    /** デフォルトのヘッダー探索行数 */
    private static final int DEFAULT_HEADER_SEARCH_ROWS = 10;
    
    /** StreamingReaderのデフォルト行キャッシュサイズ（メモリに保持する行数） */
    private static final int DEFAULT_ROW_CACHE_SIZE = 100;
    
    /** StreamingReaderのデフォルトバッファサイズ（バイト単位） */
    private static final int DEFAULT_BUFFER_SIZE = 4096;


    private final Class<T> beanClass;
    private final Path filePath;
    int sheetIndex = 0;
    String sheetName = null;
    int skipLines = 0;
    boolean usePositionMapping = false;
    String headerKeyColumn = null;
    int headerSearchRows = DEFAULT_HEADER_SEARCH_ROWS;

    private ExcelStreamReader(Class<T> beanClass, Path filePath) {
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
     * Streamを処理する（メモリ効率の良いストリーミング処理）
     *
     * @param <R> 戻り値の型
     * @param processor Streamを処理する関数
     * @return 処理結果
     * @throws IOException ファイル読み込みエラー
     */
    private <R> R process(Function<Stream<T>, R> processor) throws IOException {
        // ファイル存在チェック
        if (!Files.exists(filePath)) {
            throw new IOException("ファイルが存在しません: " + filePath);
        }
        if (!Files.isReadable(filePath)) {
            throw new IOException("ファイルを読み込めません: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             Workbook workbook = StreamingReader.builder()
                 .rowCacheSize(DEFAULT_ROW_CACHE_SIZE)
                 .bufferSize(DEFAULT_BUFFER_SIZE)
                 .open(fis)) {

            Sheet sheet;
            try {
                sheet = getSheet(workbook);
            } catch (com.github.pjfanning.xlsx.exceptions.MissingSheetException e) {
                // Streaming Readerの例外を自前の例外に変換
                if (sheetName != null) {
                    throw new SheetNotFoundException(sheetName);
                } else {
                    throw new SheetNotFoundException(sheetIndex);
                }
            }
            
            if (sheet == null) {
                if (sheetName != null) {
                    throw new SheetNotFoundException(sheetName);
                } else {
                    throw new SheetNotFoundException(sheetIndex);
                }
            }

            // ストリーミング処理用のIteratorを作成
            Iterator<T> iterator = createStreamingIterator(sheet);
            
            // IteratorをStreamに変換
            Stream<T> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, 0), 
                false
            );

            // データ行をスキップする処理
            if (skipLines > 0) {
                stream = stream.skip(skipLines);
            }

            // 呼び出し側でStreamを処理
            try {
                return processor.apply(stream);
            } catch (UncheckedExcelException e) {
                // ストリーミング処理中に発生したExcel固有の例外を元に戻す
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else if (cause instanceof Exception) {
                    throw new IOException("Excel処理中にエラーが発生しました", cause);
                } else {
                    throw e;
                }
            }

        } catch (IOException e) {
            log.error("Excelファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Excel処理中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new IOException("Excel処理中にエラーが発生しました", e);
        }
    }

    /**
     * 戻り値不要の処理用ショートカット
     *
     * @param consumer Streamを消費する処理
     * @throws IOException ファイル読み込みエラー
     */
    private void process(Consumer<Stream<T>> consumer) throws IOException {
        process(stream -> {
            consumer.accept(stream);
            return null;
        });
    }

    /**
     * ストリーミング処理用のIteratorを作成
     * 行ごとにBeanを生成することでメモリ効率を向上
     */
    private Iterator<T> createStreamingIterator(Sheet sheet) {
        return new ExcelRowIterator<>(
            sheet.iterator(), 
            beanClass, 
            headerKeyColumn, 
            headerSearchRows, 
            usePositionMapping
        );
    }

    /**
     * ワークブックからシートを取得
     */
    private Sheet getSheet(Workbook workbook) {
        if (sheetName != null) {
            return workbook.getSheet(sheetName);
        } else {
            return workbook.getSheetAt(sheetIndex);
        }
    }

    /**
     * ExcelStreamReaderのBuilderクラス
     *
     * <p>Builderパターンを使用して、ExcelStreamReaderの設定を行います。
     * メソッドチェーンで設定を積み重ね、最後に{@link #process(Function)}を呼び出すことで
     * Excelファイルを読み込みます。</p>
     */
    public static class Builder<T> {
        private final ExcelStreamReader<T> reader;
        
        private Builder(Class<T> beanClass, Path filePath) {
            this.reader = new ExcelStreamReader<>(beanClass, filePath);
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
         * <p>注意: process()メソッド実行時に以下の例外が投げられる可能性があります：
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
         * Streamを処理する（メモリ効率の良いストリーミング処理）
         *
         * @param <R> 戻り値の型
         * @param processor Streamを処理する関数
         * @return 処理結果
         * @throws IOException ファイル読み込みエラー
         */
        public <R> R process(Function<Stream<T>, R> processor) throws IOException {
            return reader.process(processor);
        }
        
        /**
         * 戻り値不要の処理用ショートカット
         *
         * @param consumer Streamを消費する処理
         * @throws IOException ファイル読み込みエラー
         */
        public void process(Consumer<Stream<T>> consumer) throws IOException {
            reader.process(consumer);
        }
    }

}
