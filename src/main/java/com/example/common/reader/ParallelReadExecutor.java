package com.example.common.reader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * 複数ファイルの並列読み込みを制御するExecutor
 */
@Slf4j
public class ParallelReadExecutor {

    /**
     * 複数ファイルを並列に読み込み、入力順序を維持して結果を結合して返す
     *
     * @param <T> 結果の要素型
     * @param filePaths 読み込むファイルのパスリスト
     * @param reader ファイル読み込み関数
     * @param parallelism 並列度（1以下の場合は逐次処理）
     * @return 結合された結果リスト
     */
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public static <T> List<T> readAll(List<Path> filePaths, Function<Path, List<T>> reader, int parallelism) {
        if (filePaths == null || filePaths.isEmpty()) {
            return new ArrayList<>();
        }

        // 並列度が1以下の場合は逐次処理（意図が明確なリテラル使用）
        if (parallelism <= 1) {
            log.debug("逐次処理で{}ファイルを読み込みます", filePaths.size());
            return filePaths.stream()
                    .map(reader)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        }

        int threadCount = Math.min(parallelism, filePaths.size());
        log.debug("並列度{} -> 実スレッド{}で{}ファイルを読み込みます", parallelism, threadCount, filePaths.size());
        // ExecutorServiceはJava 19以降でAutoCloseableを実装しているが、このプロジェクトでは明示的にshutdown()を呼ぶ
        @SuppressWarnings("PMD.CloseResource")
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<CompletableFuture<List<T>>> futures = filePaths.stream()
                    .map(path -> CompletableFuture.supplyAsync(() -> {
                        log.debug("ファイル読み込み開始: {}", path);
                        return reader.apply(path);
                    }, executor))
                    .collect(Collectors.toList());

            // すべての完了を待って、順序通りに結合
            return futures.stream()
                    .map(ParallelReadExecutor::joinUnwrapped)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        } finally {
            executor.shutdown();
        }
    }

    private static <T> T joinUnwrapped(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            throw unwrapCompletionException(e);
        }
    }

    private static RuntimeException unwrapCompletionException(CompletionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        if (cause instanceof Error) {
            throw (Error) cause;
        }
        throw new RuntimeException("並列読み込み中にエラーが発生しました", cause);
    }
}

