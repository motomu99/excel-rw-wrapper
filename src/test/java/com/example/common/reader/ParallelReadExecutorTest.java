package com.example.common.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class ParallelReadExecutorTest {

    @Test
    void readAll_emptyInput_skipsReaderAndReturnsEmptyList() {
        AtomicInteger readerCalls = new AtomicInteger();
        List<String> actual = ParallelReadExecutor.readAll(List.of(), path -> {
            readerCalls.incrementAndGet();
            return List.of(path.toString());
        }, 4);

        assertTrue(actual.isEmpty());
        assertEquals(0, readerCalls.get());
    }

    @Test
    void readAll_sequentialParallelism_preservesOrderForEachFile() {
        List<Path> filePaths = List.of(Paths.get("first"), Paths.get("second"));
        List<String> expected = List.of("first", "first-2", "second", "second-2");

        List<String> actual = ParallelReadExecutor.readAll(filePaths, path -> {
            String base = path.getFileName().toString();
            return List.of(base, base + "-2");
        }, 1);

        assertEquals(expected, actual);
    }

    @Test
    void readAll_parallelMaintainsInputOrderEvenWhenTasksFinishOutOfOrder() {
        List<Path> filePaths = List.of(Paths.get("fast"), Paths.get("slow"), Paths.get("fast2"));
        List<String> expected = List.of("fast", "slow", "fast2");

        List<String> actual = ParallelReadExecutor.readAll(filePaths, path -> {
            if ("slow".equals(path.getFileName().toString())) {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return List.of(path.getFileName().toString());
        }, 3);

        assertEquals(expected, actual);
    }

    @Test
    void readAll_parallelUnwrapsRuntimeException() {
        List<Path> filePaths = List.of(Paths.get("boom"));

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                ParallelReadExecutor.readAll(filePaths, path -> {
                    throw new IllegalStateException("nope");
                }, 2));

        assertEquals("nope", thrown.getMessage());
    }

    @Test
    void readAll_parallelPropagatesErrorsDirectly() {
        List<Path> filePaths = List.of(Paths.get("oom"));

        assertThrows(OutOfMemoryError.class, () ->
                ParallelReadExecutor.readAll(filePaths, path -> {
                    throw new OutOfMemoryError("fatal");
                }, 3));
    }
}

