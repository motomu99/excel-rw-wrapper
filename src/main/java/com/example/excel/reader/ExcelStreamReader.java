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
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.example.exception.HeaderNotFoundException;
import com.example.exception.KeyColumnNotFoundException;
import com.example.exception.SheetNotFoundException;
import com.example.exception.UncheckedExcelException;
import com.github.pjfanning.xlsx.StreamingReader;
import com.github.pjfanning.xlsx.exceptions.MissingSheetException;

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
 * // 複数ファイルを順番に処理（メモリ効率維持）
 * List&lt;Person&gt; persons = ExcelStreamReader.builder(Person.class, Arrays.asList(path1, path2))
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
     * Streamを処理する（メモリ効率の良いストリーミング処理）
     *
     * @param <R> 戻り値の型
     * @param processor Streamを処理する関数
     * @return 処理結果
     * @throws IOException ファイル読み込みエラー
     */
    private <R> R process(Function<Stream<T>, R> processor) throws IOException {
        try (OpenedResource<T> resource = openResource(filePath)) {
            Stream<T> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(resource.iterator, 0), 
                false
            );
            
            if (skipLines > 0) {
                stream = stream.skip(skipLines);
            }
            
            try {
                return processor.apply(stream);
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
    private <R> R processAll(List<Path> paths, Function<Stream<T>, R> processor) throws IOException {
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

        if (skipLines > 0) {
            // 各ファイルごとにskipするのではなく、全体からskipする場合の挙動は？
            // 現状のprocess(single)実装では、ファイル単位でスキップしている。
            // MultiFileSpliterator内でopenResourceするときにskipLinesが適用されるべきか？
            // -> openResourceからはIteratorが返る。
            // -> process(single)では Iterator -> Stream -> skip となっている。
            // -> ここでも Stream -> skip にすると、「最初のファイルのn行」だけスキップされる。
            // -> 全ファイルからn行ずつスキップしたいのか、結合後の先頭n行だけか？
            // -> Builderの設定としては「この読込設定」なので、各ファイルに適用されるのが自然。
            // -> しかし MultiFileSpliterator で openResource するとき、Iteratorしか返さない。
            // -> skip処理はStreamで行っている。
            // -> Spliterator内部でskip処理を入れるのは難しい（Iteratorを回す必要がある）。
            // -> ここで stream.skip(skipLines) すると、最初のファイルの先頭だけスキップされる。
            // -> ユーザーの期待値は？ 「同じフォーマットのファイルが複数ある」 -> ヘッダーがあるなら各ファイルでスキップしたいはず！
            // -> つまり、MultiFileSpliterator 内部で、各ファイルのIteratorを作る際にスキップ処理を入れる必要がある。
            // -> openResource メソッド内でスキップ処理を行うオプションはない。
            // -> openResource が返す Iterator をラップしてスキップさせるか、
            //    Spliterator が openNext した直後に n回 next() を呼ぶか。
        }
        
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
                // 無視 (ログ済み)
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

    private OpenedResource<T> openResource(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("ファイルが存在しません: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new IOException("ファイルを読み込めません: " + path);
        }

        FileInputStream fis = new FileInputStream(path.toFile());
        Workbook workbook = null;
        
        try {
            workbook = StreamingReader.builder()
                 .rowCacheSize(DEFAULT_ROW_CACHE_SIZE)
                 .bufferSize(DEFAULT_BUFFER_SIZE)
                 .open(fis);

            Sheet sheet;
            try {
                sheet = getSheet(workbook);
            } catch (MissingSheetException e) {
                throwSheetNotFound();
                return null; // unreachable
            }
            if (sheet == null) {
                throwSheetNotFound();
            }
            
            Iterator<T> iterator = createStreamingIterator(sheet);
            
            return new OpenedResource<>(fis, workbook, iterator);
        } catch (Exception e) {
            if (workbook != null) {
                try { workbook.close(); } catch (IOException ignore) {}
            }
            try { fis.close(); } catch (IOException ignore) {}
            
            if (e instanceof IOException) throw (IOException) e;
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            throw new IOException("Excelファイルオープン中にエラー: " + path, e);
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
        final Workbook workbook;
        final Iterator<T> iterator;
        
        OpenedResource(FileInputStream fis, Workbook workbook, Iterator<T> iterator) {
            this.fis = fis;
            this.workbook = workbook;
            this.iterator = iterator;
        }
        
        @Override
        public void close() throws IOException {
            if (workbook != null) workbook.close();
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
        
        private void closeCurrent() {
            if (currentResource != null) {
                try {
                    currentResource.close();
                } catch (IOException e) {
                    log.warn("リソースクローズ中にエラー", e);
                }
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
     * メソッドチェーンで設定を積み重ね、最後に{@link #process(Function)}を呼び出すことで
     * Excelファイルを読み込みます。</p>
     */
    public static class Builder<T> {
        private final ExcelStreamReader<T> reader;
        private final List<Path> filePaths;
        
        private Builder(Class<T> beanClass, Path filePath) {
            this.reader = new ExcelStreamReader<>(beanClass, filePath);
            this.filePaths = java.util.Collections.singletonList(filePath);
        }
        
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
            if (filePaths.size() > 1) {
                return reader.processAll(filePaths, processor);
            }
            return reader.process(processor);
        }
        
        /**
         * 戻り値不要の処理用ショートカット
         *
         * @param consumer Streamを消費する処理
         * @throws IOException ファイル読み込みエラー
         */
        public void process(Consumer<Stream<T>> consumer) throws IOException {
            process(stream -> {
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
        public Builder<T> parallelism(int parallelism) {
            // 現状は実装しないが、API互換性のためにメソッドだけ用意
            if (parallelism > 1) {
                log.warn("ExcelStreamReader does not support parallel reading to avoid OOM. Processing sequentially.");
            }
            return this;
        }
    }

}
