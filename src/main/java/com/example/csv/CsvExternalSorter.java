package com.example.csv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * 大容量CSVファイル用の外部ソートユーティリティクラス
 * 
 * <p>4GB～10GB程度の大きなCSVファイルをメモリに収まるサイズのチャンクに分割し、
 * 各チャンクをソートした後、k-wayマージソートで結合します。</p>
 * 
 * <h3>使用例:</h3>
 * <pre>
 * // 基本的な使い方（文字列として各行をソート）
 * CsvExternalSorter.builder(Paths.get("large_input.csv"), Paths.get("sorted_output.csv"))
 *     .chunkSize(100_000_000)  // 100MBごとにチャンク分割
 *     .charset(CharsetType.UTF_8)
 *     .fileType(FileType.CSV)
 *     .comparator((line1, line2) -> {
 *         // 2列目でソート（例）
 *         String[] cols1 = line1.split(",");
 *         String[] cols2 = line2.split(",");
 *         return cols1[1].compareTo(cols2[1]);
 *     })
 *     .sort();
 * 
 * // 数値でソートする例
 * CsvExternalSorter.builder(inputPath, outputPath)
 *     .comparator((line1, line2) -> {
 *         String[] cols1 = line1.split(",");
 *         String[] cols2 = line2.split(",");
 *         return Integer.compare(Integer.parseInt(cols1[0]), Integer.parseInt(cols2[0]));
 *     })
 *     .sort();
 * </pre>
 * 
 * <p>このクラスは以下のアルゴリズムで動作します：</p>
 * <ol>
 *   <li>入力ファイルをチャンクサイズごとに読み込む</li>
 *   <li>各チャンクをメモリ内でソート</li>
 *   <li>ソート済みチャンクを一時ファイルに書き込む</li>
 *   <li>すべての一時ファイルをk-wayマージソートで結合</li>
 *   <li>一時ファイルをクリーンアップ</li>
 * </ol>
 */
@Slf4j
public class CsvExternalSorter {
    
    private final Path inputPath;
    private final Path outputPath;
    private long chunkSize = 100_000_000L; // デフォルト100MB
    private CharsetType charsetType = CharsetType.UTF_8;
    private FileType fileType = FileType.CSV;
    private Comparator<String> comparator = String::compareTo;
    private boolean skipHeader = true;
    private Path tempDirectory;
    
    private CsvExternalSorter(Path inputPath, Path outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"), 
                                       "csv-external-sort-" + UUID.randomUUID());
    }
    
    /**
     * Builderインスタンスを生成
     * 
     * @param inputPath 入力CSVファイルのパス
     * @param outputPath 出力CSVファイルのパス
     * @return Builderインスタンス
     */
    public static Builder builder(Path inputPath, Path outputPath) {
        return new Builder(inputPath, outputPath);
    }
    
    /**
     * 外部ソートを実行
     * 
     * @throws IOException ファイル操作エラー
     */
    public void sort() throws IOException {
        log.info("外部ソート開始: 入力={}, 出力={}, チャンクサイズ={}MB", 
                inputPath, outputPath, chunkSize / 1_000_000);
        
        try {
            // 一時ディレクトリを作成
            Files.createDirectories(tempDirectory);
            log.debug("一時ディレクトリ作成: {}", tempDirectory);
            
            // フェーズ1: ファイルを分割してソート
            List<Path> sortedChunks = splitAndSort();
            log.info("チャンク分割・ソート完了: チャンク数={}", sortedChunks.size());
            
            // フェーズ2: ソート済みチャンクをマージ
            mergeChunks(sortedChunks);
            log.info("マージ完了: 出力ファイル={}", outputPath);
            
        } finally {
            // 一時ファイルをクリーンアップ
            cleanup();
        }
    }
    
    /**
     * 入力ファイルをチャンクに分割し、各チャンクをソート
     * 
     * @return ソート済みチャンクファイルのパスリスト
     * @throws IOException ファイル操作エラー
     */
    private List<Path> splitAndSort() throws IOException {
        List<Path> chunkFiles = new ArrayList<>();
        List<String> currentChunk = new ArrayList<>();
        long currentSize = 0;
        String headerLine = null;
        
        try (BufferedReader reader = new BufferedReader(
                new FileReader(inputPath.toFile(), 
                             java.nio.charset.Charset.forName(charsetType.getCharsetName())))) {
            
            // ヘッダー行を読み込む
            if (skipHeader) {
                headerLine = reader.readLine();
                if (headerLine != null) {
                    log.debug("ヘッダー行: {}", headerLine);
                }
            }
            
            String line;
            int lineCount = 0;
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                long lineSize = line.getBytes(charsetType.getCharsetName()).length;
                
                // チャンクサイズを超えた場合、現在のチャンクをソートして保存
                if (currentSize + lineSize > chunkSize && !currentChunk.isEmpty()) {
                    Path chunkFile = sortAndSaveChunk(currentChunk, chunkFiles.size());
                    chunkFiles.add(chunkFile);
                    log.debug("チャンク保存: ファイル={}, 行数={}, サイズ={}MB", 
                            chunkFile.getFileName(), currentChunk.size(), currentSize / 1_000_000);
                    
                    currentChunk.clear();
                    currentSize = 0;
                }
                
                currentChunk.add(line);
                currentSize += lineSize;
            }
            
            // 最後のチャンクを保存
            if (!currentChunk.isEmpty()) {
                Path chunkFile = sortAndSaveChunk(currentChunk, chunkFiles.size());
                chunkFiles.add(chunkFile);
                log.debug("最終チャンク保存: ファイル={}, 行数={}, サイズ={}MB", 
                        chunkFile.getFileName(), currentChunk.size(), currentSize / 1_000_000);
            }
            
            log.info("入力ファイル読み込み完了: 総行数={}, チャンク数={}", lineCount, chunkFiles.size());
        }
        
        return chunkFiles;
    }
    
    /**
     * チャンクをソートして一時ファイルに保存
     * 
     * @param chunk ソート対象の行リスト
     * @param chunkIndex チャンクのインデックス
     * @return 保存した一時ファイルのパス
     * @throws IOException ファイル操作エラー
     */
    private Path sortAndSaveChunk(List<String> chunk, int chunkIndex) throws IOException {
        // チャンクをソート
        chunk.sort(comparator);
        
        // 一時ファイルに保存
        Path chunkFile = tempDirectory.resolve("chunk_" + chunkIndex + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(chunkFile.toFile(), 
                             java.nio.charset.Charset.forName(charsetType.getCharsetName())))) {
            for (String line : chunk) {
                writer.write(line);
                writer.newLine();
            }
        }
        
        return chunkFile;
    }
    
    /**
     * ソート済みチャンクをk-wayマージソートで結合
     * 
     * @param chunkFiles ソート済みチャンクファイルのリスト
     * @throws IOException ファイル操作エラー
     */
    private void mergeChunks(List<Path> chunkFiles) throws IOException {
        // 各チャンクファイル用のBufferedReaderを作成
        List<BufferedReader> readers = new ArrayList<>();
        PriorityQueue<ChunkLine> priorityQueue = new PriorityQueue<>(
            (a, b) -> comparator.compare(a.line, b.line)
        );
        
        try {
            // 各チャンクファイルから最初の行を読み込む
            for (int i = 0; i < chunkFiles.size(); i++) {
                BufferedReader reader = new BufferedReader(
                    new FileReader(chunkFiles.get(i).toFile(),
                                 java.nio.charset.Charset.forName(charsetType.getCharsetName())));
                readers.add(reader);
                
                String line = reader.readLine();
                if (line != null) {
                    priorityQueue.offer(new ChunkLine(line, i));
                }
            }
            
            // 出力ファイルに書き込み
            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter(outputPath.toFile(),
                                 java.nio.charset.Charset.forName(charsetType.getCharsetName())))) {
                
                // ヘッダー行を書き込む
                if (skipHeader) {
                    try (BufferedReader headerReader = new BufferedReader(
                            new FileReader(inputPath.toFile(),
                                         java.nio.charset.Charset.forName(charsetType.getCharsetName())))) {
                        String headerLine = headerReader.readLine();
                        if (headerLine != null) {
                            writer.write(headerLine);
                            writer.newLine();
                        }
                    }
                }
                
                // k-wayマージ
                int mergedLines = 0;
                while (!priorityQueue.isEmpty()) {
                    ChunkLine current = priorityQueue.poll();
                    writer.write(current.line);
                    writer.newLine();
                    mergedLines++;
                    
                    // 同じチャンクから次の行を読み込む
                    String nextLine = readers.get(current.chunkIndex).readLine();
                    if (nextLine != null) {
                        priorityQueue.offer(new ChunkLine(nextLine, current.chunkIndex));
                    }
                    
                    // 進捗ログ（100万行ごと）
                    if (mergedLines % 1_000_000 == 0) {
                        log.debug("マージ進捗: {}行処理完了", mergedLines);
                    }
                }
                
                log.info("マージ完了: 総行数={}", mergedLines);
            }
            
        } finally {
            // すべてのReaderをクローズ
            for (BufferedReader reader : readers) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.warn("Readerのクローズに失敗: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 一時ファイルとディレクトリをクリーンアップ
     */
    private void cleanup() {
        try {
            if (Files.exists(tempDirectory)) {
                Files.walk(tempDirectory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("一時ファイルの削除に失敗: path={}, error={}", path, e.getMessage());
                        }
                    });
                log.debug("一時ファイルクリーンアップ完了: {}", tempDirectory);
            }
        } catch (IOException e) {
            log.warn("一時ディレクトリのクリーンアップに失敗: {}", e.getMessage());
        }
    }
    
    /**
     * チャンク内の行とそのインデックスを保持するクラス
     */
    private static class ChunkLine {
        final String line;
        final int chunkIndex;
        
        ChunkLine(String line, int chunkIndex) {
            this.line = line;
            this.chunkIndex = chunkIndex;
        }
    }
    
    /**
     * CsvExternalSorterのBuilderクラス
     */
    public static class Builder {
        private final CsvExternalSorter sorter;
        
        private Builder(Path inputPath, Path outputPath) {
            this.sorter = new CsvExternalSorter(inputPath, outputPath);
        }
        
        /**
         * チャンクサイズを設定（バイト単位）
         * 
         * @param chunkSize チャンクサイズ（デフォルト: 100MB）
         * @return このBuilderインスタンス
         */
        public Builder chunkSize(long chunkSize) {
            sorter.chunkSize = chunkSize;
            return this;
        }
        
        /**
         * 文字セットを設定
         * 
         * @param charsetType 文字セットタイプ
         * @return このBuilderインスタンス
         */
        public Builder charset(CharsetType charsetType) {
            sorter.charsetType = charsetType;
            return this;
        }
        
        /**
         * ファイルタイプを設定
         * 
         * @param fileType ファイルタイプ
         * @return このBuilderインスタンス
         */
        public Builder fileType(FileType fileType) {
            sorter.fileType = fileType;
            return this;
        }
        
        /**
         * ソート用のComparatorを設定
         * 
         * @param comparator 行を比較するComparator
         * @return このBuilderインスタンス
         */
        public Builder comparator(Comparator<String> comparator) {
            sorter.comparator = comparator;
            return this;
        }
        
        /**
         * ヘッダー行をスキップするかどうかを設定
         * 
         * @param skipHeader ヘッダー行をスキップする場合true（デフォルト: true）
         * @return このBuilderインスタンス
         */
        public Builder skipHeader(boolean skipHeader) {
            sorter.skipHeader = skipHeader;
            return this;
        }
        
        /**
         * 一時ディレクトリを設定
         * 
         * @param tempDirectory 一時ファイルを保存するディレクトリ
         * @return このBuilderインスタンス
         */
        public Builder tempDirectory(Path tempDirectory) {
            sorter.tempDirectory = tempDirectory;
            return this;
        }
        
        /**
         * 外部ソートを実行
         * 
         * @throws IOException ファイル操作エラー
         */
        public void sort() throws IOException {
            sorter.sort();
        }
    }
}

