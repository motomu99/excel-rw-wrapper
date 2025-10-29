package com.example.csv;

import static org.junit.jupiter.api.Assertions.*;

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * CsvExternalSorterのテストクラス
 */
class CsvExternalSorterTest {

    @TempDir
    Path tempDir;
    
    private Path inputPath;
    private Path outputPath;
    
    @BeforeEach
    void setUp() throws IOException {
        inputPath = tempDir.resolve("input.csv");
        outputPath = tempDir.resolve("output.csv");
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // テスト後のクリーンアップ
        if (Files.exists(inputPath)) {
            Files.delete(inputPath);
        }
        if (Files.exists(outputPath)) {
            Files.delete(outputPath);
        }
    }
    
    /**
     * 基本的な文字列ソートのテスト
     */
    @Test
    void testBasicSort() throws IOException {
        // テストデータを作成
        createTestFile(inputPath, List.of(
            "name,age,city",
            "Charlie,25,Tokyo",
            "Alice,30,Osaka",
            "Bob,20,Kyoto"
        ));
        
        // ソート実行（name列でソート）
        CsvExternalSorter.builder(inputPath, outputPath)
            .chunkSize(1000) // 小さいチャンクサイズでテスト
            .comparator((line1, line2) -> {
                String name1 = line1.split(",")[0];
                String name2 = line2.split(",")[0];
                return name1.compareTo(name2);
            })
            .sort();
        
        // 結果を検証
        List<String> result = readFile(outputPath);
        assertEquals(4, result.size());
        assertEquals("name,age,city", result.get(0)); // ヘッダー
        assertEquals("Alice,30,Osaka", result.get(1));
        assertEquals("Bob,20,Kyoto", result.get(2));
        assertEquals("Charlie,25,Tokyo", result.get(3));
    }
    
    /**
     * 数値列でのソートテスト
     */
    @Test
    void testNumericSort() throws IOException {
        // テストデータを作成
        createTestFile(inputPath, List.of(
            "id,name,score",
            "3,Alice,85",
            "1,Bob,95",
            "2,Charlie,75"
        ));
        
        // ソート実行（id列でソート）
        CsvExternalSorter.builder(inputPath, outputPath)
            .chunkSize(1000)
            .comparator((line1, line2) -> {
                int id1 = Integer.parseInt(line1.split(",")[0]);
                int id2 = Integer.parseInt(line2.split(",")[0]);
                return Integer.compare(id1, id2);
            })
            .sort();
        
        // 結果を検証
        List<String> result = readFile(outputPath);
        assertEquals(4, result.size());
        assertEquals("id,name,score", result.get(0)); // ヘッダー
        assertEquals("1,Bob,95", result.get(1));
        assertEquals("2,Charlie,75", result.get(2));
        assertEquals("3,Alice,85", result.get(3));
    }
    
    /**
     * 降順ソートのテスト
     */
    @Test
    void testDescendingSort() throws IOException {
        // テストデータを作成
        createTestFile(inputPath, List.of(
            "name,score",
            "Alice,85",
            "Bob,95",
            "Charlie,75"
        ));
        
        // ソート実行（score列で降順ソート）
        CsvExternalSorter.builder(inputPath, outputPath)
            .chunkSize(1000)
            .comparator((line1, line2) -> {
                int score1 = Integer.parseInt(line1.split(",")[1]);
                int score2 = Integer.parseInt(line2.split(",")[1]);
                return Integer.compare(score2, score1); // 降順
            })
            .sort();
        
        // 結果を検証
        List<String> result = readFile(outputPath);
        assertEquals(4, result.size());
        assertEquals("name,score", result.get(0)); // ヘッダー
        assertEquals("Bob,95", result.get(1));
        assertEquals("Alice,85", result.get(2));
        assertEquals("Charlie,75", result.get(3));
    }
    
    /**
     * 大量データのソートテスト（複数チャンクに分割）
     */
    @Test
    void testLargeDataSort() throws IOException {
        // 1000行のテストデータを作成
        List<String> lines = new ArrayList<>();
        lines.add("id,value");
        for (int i = 1000; i >= 1; i--) {
            lines.add(i + ",value" + i);
        }
        createTestFile(inputPath, lines);
        
        // ソート実行（小さいチャンクサイズで複数チャンクに分割）
        CsvExternalSorter.builder(inputPath, outputPath)
            .chunkSize(500) // 500バイトごとにチャンク分割
            .comparator((line1, line2) -> {
                int id1 = Integer.parseInt(line1.split(",")[0]);
                int id2 = Integer.parseInt(line2.split(",")[0]);
                return Integer.compare(id1, id2);
            })
            .sort();
        
        // 結果を検証
        List<String> result = readFile(outputPath);
        assertEquals(1001, result.size());
        assertEquals("id,value", result.get(0)); // ヘッダー
        assertEquals("1,value1", result.get(1)); // 最小値
        assertEquals("500,value500", result.get(500)); // 中間値
        assertEquals("1000,value1000", result.get(1000)); // 最大値
    }
    
    /**
     * ヘッダーなしのソートテスト
     */
    @Test
    void testSortWithoutHeader() throws IOException {
        // ヘッダーなしのテストデータを作成
        createTestFile(inputPath, List.of(
            "Charlie,25,Tokyo",
            "Alice,30,Osaka",
            "Bob,20,Kyoto"
        ));
        
        // ソート実行（ヘッダーなし）
        CsvExternalSorter.builder(inputPath, outputPath)
            .chunkSize(1000)
            .skipHeader(false)
            .comparator((line1, line2) -> {
                String name1 = line1.split(",")[0];
                String name2 = line2.split(",")[0];
                return name1.compareTo(name2);
            })
            .sort();
        
        // 結果を検証
        List<String> result = readFile(outputPath);
        assertEquals(3, result.size());
        assertEquals("Alice,30,Osaka", result.get(0));
        assertEquals("Bob,20,Kyoto", result.get(1));
        assertEquals("Charlie,25,Tokyo", result.get(2));
    }
    
    /**
     * TSVファイルのソートテスト
     */
    @Test
    void testTsvSort() throws IOException {
        // TSVテストデータを作成（タブ区切り）
        createTestFile(inputPath, List.of(
            "name\tage\tcity",
            "Charlie\t25\tTokyo",
            "Alice\t30\tOsaka",
            "Bob\t20\tKyoto"
        ));
        
        // ソート実行
        CsvExternalSorter.builder(inputPath, outputPath)
            .chunkSize(1000)
            .fileType(FileType.TSV)
            .comparator((line1, line2) -> {
                String name1 = line1.split("\t")[0];
                String name2 = line2.split("\t")[0];
                return name1.compareTo(name2);
            })
            .sort();
        
        // 結果を検証
        List<String> result = readFile(outputPath);
        assertEquals(4, result.size());
        assertEquals("name\tage\tcity", result.get(0)); // ヘッダー
        assertTrue(result.get(1).startsWith("Alice"));
        assertTrue(result.get(2).startsWith("Bob"));
        assertTrue(result.get(3).startsWith("Charlie"));
    }
    
    /**
     * 空ファイルのテスト
     */
    @Test
    void testEmptyFile() throws IOException {
        // 空ファイルを作成
        createTestFile(inputPath, List.of());
        
        // ソート実行
        CsvExternalSorter.builder(inputPath, outputPath)
            .chunkSize(1000)
            .sort();
        
        // 結果を検証（ヘッダーのみ、または空）
        List<String> result = readFile(outputPath);
        assertTrue(result.isEmpty());
    }
    
    /**
     * ヘッダーのみのファイルテスト
     */
    @Test
    void testHeaderOnlyFile() throws IOException {
        // ヘッダーのみのファイルを作成
        createTestFile(inputPath, List.of("name,age,city"));
        
        // ソート実行
        CsvExternalSorter.builder(inputPath, outputPath)
            .chunkSize(1000)
            .sort();
        
        // 結果を検証
        List<String> result = readFile(outputPath);
        assertEquals(1, result.size());
        assertEquals("name,age,city", result.get(0));
    }
    
    /**
     * 複数列でのソートテスト
     */
    @Test
    void testMultiColumnSort() throws IOException {
        // テストデータを作成
        createTestFile(inputPath, List.of(
            "city,name,age",
            "Tokyo,Bob,25",
            "Tokyo,Alice,30",
            "Osaka,Charlie,20",
            "Osaka,Alice,25"
        ));
        
        // ソート実行（city列 → name列の順）
        CsvExternalSorter.builder(inputPath, outputPath)
            .chunkSize(1000)
            .comparator((line1, line2) -> {
                String[] cols1 = line1.split(",");
                String[] cols2 = line2.split(",");
                
                // まずcity列で比較
                int cityCompare = cols1[0].compareTo(cols2[0]);
                if (cityCompare != 0) {
                    return cityCompare;
                }
                
                // cityが同じならname列で比較
                return cols1[1].compareTo(cols2[1]);
            })
            .sort();
        
        // 結果を検証
        List<String> result = readFile(outputPath);
        assertEquals(5, result.size());
        assertEquals("city,name,age", result.get(0)); // ヘッダー
        assertEquals("Osaka,Alice,25", result.get(1));
        assertEquals("Osaka,Charlie,20", result.get(2));
        assertEquals("Tokyo,Alice,30", result.get(3));
        assertEquals("Tokyo,Bob,25", result.get(4));
    }
    
    /**
     * テストファイルを作成するヘルパーメソッド
     */
    private void createTestFile(Path path, List<String> lines) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }
    
    /**
     * ファイルを読み込むヘルパーメソッド
     */
    private List<String> readFile(Path path) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}

