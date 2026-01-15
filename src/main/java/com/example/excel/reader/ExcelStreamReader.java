package com.example.excel.reader;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Sheet;
import org.dhatim.fastexcel.reader.Row;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.example.common.mapping.MappingStrategyDetector;
import com.example.exception.HeaderNotFoundException;
import com.example.exception.KeyColumnNotFoundException;
import com.example.exception.SheetNotFoundException;
import com.example.exception.UncheckedExcelException;

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
 *     .extract(stream -&gt; stream.collect(Collectors.toList()));
 *
 * // ヘッダー行を自動検出（上から10行以内で「名前」列を探す）
 * List&lt;Person&gt; persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
 *     .headerKey("名前")
 *     .extract(stream -&gt; stream.collect(Collectors.toList()));
 *
 * // 複数ファイルを順番に処理（メモリ効率維持）
 * List&lt;Person&gt; persons = ExcelStreamReader.builder(Person.class, Arrays.asList(path1, path2))
 *     .extract(stream -&gt; stream.collect(Collectors.toList()));
 *
 * // 一括読み込み（extract()/consume()を使わない）
 * List&lt;Person&gt; persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
 *     .sheetIndex(0)
 *     .skip(1)
 *     .read();
 * </pre>
 */
@Slf4j
public class ExcelStreamReader<T> {

    /** デフォルトのヘッダー探索行数 */
    private static final int DEFAULT_HEADER_SEARCH_ROWS = 10;


    private final Class<T> beanClass;
    private final Path filePath;
    int sheetIndex = 0;
    String sheetName = null;
    int skipLines = 0;
    Boolean usePositionMapping = null;
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
     * マッピング戦略を決定する
     */
    private void resolveMappingStrategy() {
        if (usePositionMapping == null) {
            usePositionMapping = MappingStrategyDetector.detectUsePositionMapping(beanClass)
                    .orElse(false); // デフォルトはヘッダーベース
        }
    }

    /**
     * Streamを処理して結果を返す（メモリ効率の良いストリーミング処理）
     *
     * @param <R> 戻り値の型
     * @param processor Streamを処理する関数
     * @return 処理結果
     * @throws IOException ファイル読み込みエラー
     */
    private <R> R extract(Function<Stream<T>, R> processor) throws IOException {
        resolveMappingStrategy();
        try (OpenedResource<T> resource = openResource(filePath)) {
            // StreamはBaseStreamを継承しており、AutoCloseableを実装しているためtry-with-resourcesで管理する
            try (Stream<T> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(resource.iterator, 0), 
                false
            )) {
                Stream<T> processedStream = skipLines > 0 ? stream.skip(skipLines) : stream;
                return processor.apply(processedStream);
            } catch (UncheckedExcelException e) {
                throw toIOException(e);
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
     * 複数ファイルを順次処理する
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    private <R> R processAll(List<Path> paths, Function<Stream<T>, R> processor) throws IOException {
        resolveMappingStrategy();
        // カスタムSpliteratorで遅延読み込みStreamを作成
        MultiFileSpliterator<T> spliterator = new MultiFileSpliterator<>(paths, this);
        
        // ストリーム作成（onCloseでリソース開放を保証）
        Stream<T> stream = StreamSupport.stream(spliterator, false)
            .onClose(() -> {
                try {
                    spliterator.close();
                } catch (Exception e) {
                    log.error("ストリームクローズ中にエラー", e);
                }
            });

        // 修正: スキップロジックは MultiFileSpliterator 内でハンドルする（各ファイルごとに適用）
        // ただし、this.skipLines は MultiFileSpliterator に渡される templateReader に含まれている。

        try {
            return processor.apply(stream);
        } catch (UncheckedExcelException e) {
            throw toIOException(e);
        } finally {
            // processorがStreamをクローズしなかった場合の保険
            // (Stream.onCloseはStream.close()が呼ばれた時のみ実行される)
            // しかしここでの明示的closeはStreamが既に閉じられている場合に副作用があるかも？
            // 通常は try-with-resources で stream を受けるか、processor内で閉じることを期待する。
            // ここでは spliterator 自体を閉じる。
            try {
                spliterator.close();
            } catch (Exception e) {
                // クローズ時のエラーは無視（ログ済み）
            }
        }
    }

    private static IOException toIOException(UncheckedExcelException e) {
        Throwable cause = e.getCause();
        if (cause instanceof IOException) {
            return (IOException) cause;
        }
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        if (cause instanceof Exception) {
            return new IOException("Excel処理中にエラーが発生しました", cause);
        }
        throw e;
    }
    
    private ExcelStreamReader<T> cloneConfig(Path newPath) {
        ExcelStreamReader<T> clone = new ExcelStreamReader<>(this.beanClass, newPath);
        clone.sheetIndex = this.sheetIndex;
        clone.sheetName = this.sheetName;
        clone.skipLines = this.skipLines;
        clone.usePositionMapping = this.usePositionMapping;
        clone.headerKeyColumn = this.headerKeyColumn;
        clone.headerSearchRows = this.headerSearchRows;
        return clone;
    }


    @SuppressWarnings({"PMD.AvoidInstanceofChecksInCatchClause", "PMD.EmptyCatchBlock"})
    private OpenedResource<T> openResource(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("ファイルが存在しません: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("ファイルを読み込めません: " + path);
        }

        FileInputStream fis = new FileInputStream(path.toFile());
        ReadableWorkbook workbook = null;
        Stream<Row> rowStream = null;
        
        try {
            workbook = new ReadableWorkbook(fis);

            Sheet sheet = getSheet(workbook);
            if (sheet == null) {
                throwSheetNotFound();
            }
            
            rowStream = sheet.openStream();
            Iterator<Row> rowIterator = rowStream.iterator();
            Iterator<T> iterator = createStreamingIterator(rowIterator);
            
            return new OpenedResource<>(fis, workbook, rowStream, iterator);
        } catch (org.dhatim.fastexcel.reader.ExcelReaderException e) {
            // fastexcelのExcelReaderExceptionをIOExceptionに変換
            throw new IOException("Excelファイル読み込みエラー: " + path, e);
        } catch (Exception e) {
            // エラー発生時のクリーンアップ: リソースクローズ時のエラーは無視する（意図的な実装）
            if (rowStream != null) {
                try { 
                    rowStream.close(); 
                } catch (Exception ignore) {
                    // クローズ時のエラーは無視（既に例外が発生しているため）
                }
            }
            if (workbook != null) {
                try { 
                    workbook.close(); 
                } catch (Exception ignore) {
                    // クローズ時のエラーは無視（既に例外が発生しているため）
                }
            }
            try { 
                fis.close(); 
            } catch (IOException ignore) {
                // クローズ時のエラーは無視（既に例外が発生しているため）
            }
            
            // 例外の型に応じて再スロー（catch句の外でのinstanceofチェックは意図的な実装）
            if (e instanceof IOException) throw (IOException) e;
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new IOException("Excelファイルオープン中にエラー: " + path, e);
        }
    }
    
    /**
     * 列数チェックを有効にしたリソースを開く
     */
    @SuppressWarnings({"PMD.AvoidInstanceofChecksInCatchClause", "PMD.EmptyCatchBlock"})
    private OpenedResourceWithValidation<T> openResourceWithValidation(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("ファイルが存在しません: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("ファイルを読み込めません: " + path);
        }

        FileInputStream fis = new FileInputStream(path.toFile());
        ReadableWorkbook workbook = null;
        Stream<Row> rowStream = null;
        
        try {
            workbook = new ReadableWorkbook(fis);

            Sheet sheet = getSheet(workbook);
            if (sheet == null) {
                throwSheetNotFound();
            }
            
            rowStream = sheet.openStream();
            Iterator<Row> rowIterator = rowStream.iterator();
            FastExcelRowIterator<T> iterator = createStreamingIteratorWithValidation(rowIterator);
            
            return new OpenedResourceWithValidation<>(fis, workbook, rowStream, iterator);
        } catch (org.dhatim.fastexcel.reader.ExcelReaderException e) {
            // fastexcelのExcelReaderExceptionをIOExceptionに変換
            throw new IOException("Excelファイル読み込みエラー: " + path, e);
        } catch (Exception e) {
            // エラー発生時のクリーンアップ: リソースクローズ時のエラーは無視する（意図的な実装）
            if (rowStream != null) {
                try { 
                    rowStream.close(); 
                } catch (Exception ignore) {
                    // クローズ時のエラーは無視（既に例外が発生しているため）
                }
            }
            if (workbook != null) {
                try { 
                    workbook.close(); 
                } catch (Exception ignore) {
                    // クローズ時のエラーは無視（既に例外が発生しているため）
                }
            }
            try { 
                fis.close(); 
            } catch (IOException ignore) {
                // クローズ時のエラーは無視（既に例外が発生しているため）
            }
            
            // 例外の型に応じて再スロー（catch句の外でのinstanceofチェックは意図的な実装）
            if (e instanceof IOException) throw (IOException) e;
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new IOException("Excelファイルオープン中にエラー: " + path, e);
        }
    }

    /**
     * ストリーミング処理用のIteratorを作成
     * 行ごとにBeanを生成することでメモリ効率を向上
     */
    private Iterator<T> createStreamingIterator(Iterator<Row> rowIterator) {
        return new FastExcelRowIterator<>(
            rowIterator, 
            beanClass, 
            headerKeyColumn, 
            headerSearchRows, 
            usePositionMapping,
            usePositionMapping, // 位置ベースマッピングの場合は最初の行もデータとして扱う
            false // 列数チェックはデフォルトで無効（後方互換性のため）
        );
    }
    
    /**
     * 列数チェックを有効にしたストリーミング処理用のIteratorを作成
     * 行ごとにBeanを生成することでメモリ効率を向上
     */
    private FastExcelRowIterator<T> createStreamingIteratorWithValidation(Iterator<Row> rowIterator) {
        return new FastExcelRowIterator<>(
            rowIterator, 
            beanClass, 
            headerKeyColumn, 
            headerSearchRows, 
            usePositionMapping,
            usePositionMapping, // 位置ベースマッピングの場合は最初の行もデータとして扱う
            true // 列数チェックを有効化
        );
    }

    /**
     * ワークブックからシートを取得
     */
    private Sheet getSheet(ReadableWorkbook workbook) {
        if (sheetName != null) {
            return workbook.findSheet(sheetName).orElse(null);
        } else {
            return workbook.getSheet(sheetIndex).orElse(null);
        }
    }

    private void throwSheetNotFound() throws SheetNotFoundException {
        if (sheetName != null) {
            throw new SheetNotFoundException(sheetName);
        } else {
            throw new SheetNotFoundException(sheetIndex);
        }
    }
    
    // 内部クラス定義
    
    private static class OpenedResource<T> implements AutoCloseable {
        final FileInputStream fis;
        final ReadableWorkbook workbook;
        final Stream<Row> rowStream;
        final Iterator<T> iterator;
        
        OpenedResource(FileInputStream fis, ReadableWorkbook workbook, Stream<Row> rowStream, Iterator<T> iterator) {
            this.fis = fis;
            this.workbook = workbook;
            this.rowStream = rowStream;
            this.iterator = iterator;
        }
        
        @Override
        public void close() throws IOException {
            if (rowStream != null) {
                try {
                    rowStream.close();
                } catch (Exception e) {
                    // クローズ時のエラーは無視
                }
            }
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    // クローズ時のエラーは無視
                }
            }
            if (fis != null) fis.close();
        }
    }
    
    private static class OpenedResourceWithValidation<T> implements AutoCloseable {
        final FileInputStream fis;
        final ReadableWorkbook workbook;
        final Stream<Row> rowStream;
        final FastExcelRowIterator<T> iterator;
        
        OpenedResourceWithValidation(FileInputStream fis, ReadableWorkbook workbook, Stream<Row> rowStream, FastExcelRowIterator<T> iterator) {
            this.fis = fis;
            this.workbook = workbook;
            this.rowStream = rowStream;
            this.iterator = iterator;
        }
        
        @Override
        public void close() throws IOException {
            if (rowStream != null) {
                try {
                    rowStream.close();
                } catch (Exception e) {
                    // クローズ時のエラーは無視
                }
            }
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (Exception e) {
                    // クローズ時のエラーは無視
                }
            }
            if (fis != null) fis.close();
        }
    }
    
    private static class MultiFileSpliterator<T> extends Spliterators.AbstractSpliterator<T> implements AutoCloseable {
        private final Iterator<Path> fileIterator;
        private final ExcelStreamReader<T> templateReader;
        
        private OpenedResource<T> currentResource;
        
        MultiFileSpliterator(List<Path> paths, ExcelStreamReader<T> templateReader) {
            super(Long.MAX_VALUE, 0);
            this.fileIterator = paths.iterator();
            this.templateReader = templateReader;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            if (currentResource != null && currentResource.iterator.hasNext()) {
                action.accept(currentResource.iterator.next());
                return true;
            }
            
            // 現在のリソースを閉じる
            closeCurrent();
            
            // 次のファイルへ
            if (fileIterator.hasNext()) {
                Path nextPath = fileIterator.next();
                try {
                    // テンプレートから設定をコピーしたReaderを作成し、リソースを開く
                    // cloneConfigはprivateだが、同じクラス内（static inner classも含む）ならアクセス可能？
                    // -> ExcelStreamReader.this にアクセスできない（staticなので）。
                    // -> templateReader インスタンス経由ならアクセス可能。
                    ExcelStreamReader<T> reader = templateReader.cloneConfig(nextPath);
                    currentResource = reader.openResource(nextPath);
                    
                    // スキップ行数の処理
                    // 各ファイルごとにヘッダー行などをスキップする
                    int skip = reader.skipLines;
                    while (skip > 0 && currentResource.iterator.hasNext()) {
                        currentResource.iterator.next();
                        skip--;
                    }
                    
                    return tryAdvance(action);
                } catch (IOException e) {
                    throw new UncheckedExcelException(new IOException("ファイル読み込みエラー: " + nextPath, e));
                }
            }
            
            return false;
        }
        
        @SuppressWarnings("PMD.NullAssignment")
        private void closeCurrent() {
            if (currentResource != null) {
                try {
                    currentResource.close();
                } catch (IOException e) {
                    log.warn("リソースクローズ中にエラー", e);
                }
                // リソースをnullに設定してクリーンアップ（意図的な実装）
                currentResource = null;
            }
        }

        @Override
        public void close() {
            closeCurrent();
        }
    }

    /**
     * ExcelStreamReaderのBuilderクラス
     *
     * <p>Builderパターンを使用して、ExcelStreamReaderの設定を行います。
     * メソッドチェーンで設定を積み重ね、最後に{@link #extract(Function)}または{@link #consume(Consumer)}を呼び出すことで
     * Excelファイルを読み込みます。</p>
     */
    public static class Builder<T> {
        private final ExcelStreamReader<T> reader;
        private final List<Path> filePaths;
        
        private Builder(Class<T> beanClass, Path filePath) {
            this.reader = new ExcelStreamReader<>(beanClass, filePath);
            this.filePaths = java.util.Collections.singletonList(filePath);
        }
        
        /**
         * Builderコンストラクタ
         * 
         * <p>引数の検証のために例外を投げるが、これは設計上の意図である。</p>
         */
        @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
        private Builder(Class<T> beanClass, List<Path> filePaths) {
            if (filePaths == null || filePaths.isEmpty()) {
                throw new IllegalArgumentException("filePaths must not be empty");
            }
            this.reader = new ExcelStreamReader<>(beanClass, filePaths.get(0));
            this.filePaths = filePaths;
        }
        
        /**
         * シートのインデックスを設定（0から始まる）
         *
         * @param sheetIndex シートのインデックス
         * @return このBuilderインスタンス
         */
        @SuppressWarnings("PMD.NullAssignment")
        public Builder<T> sheetIndex(int sheetIndex) {
            reader.sheetIndex = sheetIndex;
            // sheetIndexとsheetNameは排他的に使用するため、一方をnullにする（意図的な実装）
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
         * <p>注意: extract()/consume()メソッド実行時に以下の例外が投げられる可能性があります：
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
         * Streamを処理して結果を返す（メモリ効率の良いストリーミング処理）
         *
         * @param <R> 戻り値の型
         * @param processor Streamを処理する関数
         * @return 処理結果
         * @throws IOException ファイル読み込みエラー
         */
        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        public <R> R extract(Function<Stream<T>, R> processor) throws IOException {
            // 複数ファイルの場合は並列処理（意図が明確なリテラル使用）
            final int singleFileThreshold = 1;
            if (filePaths.size() > singleFileThreshold) {
                return reader.processAll(filePaths, processor);
            }
            return reader.extract(processor);
        }
        
        /**
         * 戻り値不要の処理用ショートカット
         *
         * @param consumer Streamを消費する処理
         * @throws IOException ファイル読み込みエラー
         */
        public void consume(Consumer<Stream<T>> consumer) throws IOException {
            extract(stream -> {
                consumer.accept(stream);
                return null;
            });
        }
        
        /**
         * 並列度を設定（Excel読み込みでは現在は無視され、常に順次処理となります）
         * 
         * <p>Excelのストリーミング読み込みは大量のメモリを消費するため、
         * 安全のために順次処理のみをサポートしています。</p>
         * 
         * @param parallelism 並列度
         * @return このBuilderインスタンス
         */
        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        public Builder<T> parallelism(int parallelism) {
            // 現状は実装しないが、API互換性のためにメソッドだけ用意
            // 並列度が1より大きい場合は警告（意図が明確なリテラル使用）
            final int sequentialThreshold = 1;
            if (parallelism > sequentialThreshold) {
                log.warn("ExcelStreamReader does not support parallel reading to avoid OOM. Processing sequentially.");
            }
            return this;
        }
        
        /**
         * 列数チェックを有効にしてExcelファイルを読み込み、エラー行も含めた結果を返す
         * 
         * <p>列数が不一致の行はスキップされ、エラー情報として記録されます。
         * 処理は最後まで続行され、成功した行とエラー行の情報が返されます。</p>
         * 
         * @return ExcelReadResult（成功した行のデータとエラー行の情報を含む）
         * @throws IOException ファイル読み込みエラー
         */
        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        public ExcelReadResult<T> readWithValidation() throws IOException {
            // 複数ファイルの場合は未サポート（意図が明確なリテラル使用）
            final int singleFileThreshold = 1;
            if (filePaths.size() > singleFileThreshold) {
                throw new IOException("列数チェック機能は単一ファイルのみサポートしています");
            }
            
            reader.resolveMappingStrategy();
            try (OpenedResourceWithValidation<T> resource = reader.openResourceWithValidation(reader.filePath)) {
                List<T> data = new java.util.ArrayList<>();
                try (Stream<T> stream = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(resource.iterator, 0), 
                    false
                )) {
                    Stream<T> processedStream = reader.skipLines > 0 ? stream.skip(reader.skipLines) : stream;
                    processedStream.forEach(data::add);
                } catch (UncheckedExcelException e) {
                    throw reader.toIOException(e);
                }
                
                List<ExcelReadError> errors = resource.iterator.getErrors();
                return new ExcelReadResult<>(data, errors);
            } catch (IOException e) {
                log.error("Excelファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", 
                         reader.filePath, e.getMessage(), e);
                throw e;
            } catch (Exception e) {
                log.error("Excel処理中にエラーが発生: ファイルパス={}, エラー={}", 
                         reader.filePath, e.getMessage(), e);
                throw new IOException("Excel処理中にエラーが発生しました", e);
            }
        }
        
    }

}
