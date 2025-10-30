package com.example.excel.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Spliterators;
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
 * ビルダーパターンを使用してExcel読み込み処理を抽象化
 *
 * 使用例:
 * <pre>
 * // 基本的な使用方法
 * List&lt;Person&gt; persons = ExcelStreamReader.of(Person.class, Paths.get("sample.xlsx"))
 *     .sheetIndex(0)
 *     .skip(1)
 *     .process(stream -&gt; stream.collect(Collectors.toList()));
 *
 * // ヘッダー行を自動検出（上から10行以内で「名前」列を探す）
 * List&lt;Person&gt; persons = ExcelStreamReader.of(Person.class, Paths.get("sample.xlsx"))
 *     .headerKey("名前")
 *     .process(stream -&gt; stream.collect(Collectors.toList()));
 *
 * // ヘッダー行の探索範囲を20行に拡張
 * List&lt;Person&gt; persons = ExcelStreamReader.of(Person.class, Paths.get("sample.xlsx"))
 *     .headerKey("名前")
 *     .headerSearchRows(20)
 *     .process(stream -&gt; stream.collect(Collectors.toList()));
 * </pre>
 */
@Slf4j
public class ExcelStreamReader<T> {

    /** デフォルトのヘッダー探索行数 */
    private static final int DEFAULT_HEADER_SEARCH_ROWS = 10;


    private final Class<T> beanClass;
    private final Path filePath;
    private int sheetIndex = 0;
    private String sheetName = null;
    private int skipLines = 0;
    private boolean usePositionMapping = false;
    private String headerKeyColumn = null;
    private int headerSearchRows = DEFAULT_HEADER_SEARCH_ROWS;

    private ExcelStreamReader(Class<T> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }

    /**
     * ExcelStreamReaderのインスタンスを作成
     *
     * @param <T> Beanの型
     * @param beanClass マッピング先のBeanクラス
     * @param filePath Excelファイルのパス
     * @return ExcelStreamReaderのインスタンス
     */
    public static <T> ExcelStreamReader<T> of(Class<T> beanClass, Path filePath) {
        return new ExcelStreamReader<>(beanClass, filePath);
    }

    /**
     * シートのインデックスを設定（0から始まる）
     *
     * @param sheetIndex シートのインデックス
     * @return このインスタンス
     */
    public ExcelStreamReader<T> sheetIndex(int sheetIndex) {
        this.sheetIndex = sheetIndex;
        this.sheetName = null;
        return this;
    }

    /**
     * シート名を設定
     *
     * @param sheetName シート名
     * @return このインスタンス
     */
    public ExcelStreamReader<T> sheetName(String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    /**
     * スキップする行数を設定
     *
     * @param lines スキップする行数
     * @return このインスタンス
     */
    public ExcelStreamReader<T> skip(int lines) {
        this.skipLines = lines;
        return this;
    }

    /**
     * 位置ベースマッピングを使用
     *
     * @return このインスタンス
     */
    public ExcelStreamReader<T> usePositionMapping() {
        this.usePositionMapping = true;
        return this;
    }

    /**
     * ヘッダーベースマッピングを使用（デフォルト）
     *
     * @return このインスタンス
     */
    public ExcelStreamReader<T> useHeaderMapping() {
        this.usePositionMapping = false;
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
     * @return このインスタンス
     */
    public ExcelStreamReader<T> headerKey(String keyColumnName) {
        this.headerKeyColumn = keyColumnName;
        return this;
    }

    /**
     * ヘッダー行を探索する最大行数を設定（デフォルト: 10行）
     * headerKey()と組み合わせて使用する
     *
     * @param rows 探索する最大行数
     * @return このインスタンス
     */
    public ExcelStreamReader<T> headerSearchRows(int rows) {
        this.headerSearchRows = rows;
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
        // ファイル存在チェック
        if (!Files.exists(filePath)) {
            throw new IOException("ファイルが存在しません: " + filePath);
        }
        if (!Files.isReadable(filePath)) {
            throw new IOException("ファイルを読み込めません: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             Workbook workbook = StreamingReader.builder()
                 .rowCacheSize(100)    // メモリに保持する行数
                 .bufferSize(4096)      // バッファサイズ
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

}
