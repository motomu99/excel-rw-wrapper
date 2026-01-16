package com.example.csv.sorter;

import com.example.csv.sorter.LargeDataGroupingSorter;
import com.example.model.Person;
import com.example.model.PersonWithGrouping;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🔥 大量データグルーピング＆ソート処理のデモ 🔥
 * 
 * <p>5GB級の大量CSVファイルをメモリ効率的に処理する方法を紹介</p>
 */
public class LargeDataGroupingSorterDemo {
    
    @AfterAll
    static void tearDown() throws IOException {
        // テスト終了時に生成されたファイルを削除
        Path[] csvFiles = {
            Paths.get("target/large_grouping_test.csv"),
            Paths.get("target/large_grouping_test2.csv"),
            Paths.get("target/large_grouping_test3.csv"),
            Paths.get("target/large_grouping_test4.csv")
        };
        
        for (Path csvFile : csvFiles) {
            if (Files.exists(csvFile)) {
                try {
                    Files.delete(csvFile);
                } catch (IOException e) {
                    // 削除に失敗しても続行（ファイルがロックされている場合など）
                }
            }
        }
        
        // grouped_outputディレクトリを削除
        Path outputDir = Paths.get("target/grouped_output");
        if (Files.exists(outputDir)) {
            try {
                Files.walk(outputDir)
                    .sorted((a, b) -> b.compareTo(a)) // ディレクトリを先に削除するため逆順
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // 削除に失敗しても続行
                        }
                    });
            } catch (IOException e) {
                // 削除に失敗しても続行
            }
        }
    }
    
    @Test
    @DisplayName("🔥 パターン1: Lambda指定でグルーピング＆ソート")
    void pattern1_lambdaGroupingAndSorting() throws IOException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔥 パターン1: Lambda指定 🔥");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        
        // テストデータ作成（10万行のCSV）
        Path csvPath = Paths.get("target/large_grouping_test.csv");
        createTestCsv(csvPath, 100000);
        System.out.println("✅ テストCSV作成完了: " + csvPath);
        System.out.println();
        
        // グルーピング＆ソート処理
        System.out.println("📊 グルーピング＆ソート処理開始...");
        long startTime = System.currentTimeMillis();
        
        LargeDataGroupingSorter.of(Person.class, csvPath)
            .groupBy(Person::getOccupation)                      // 職業でグルーピング
            .sortBy(Comparator.comparingInt(Person::getAge))     // 年齢でソート
            .processGroups((groupKey, personStream) -> {
                System.out.println();
                System.out.println("  📁 グループ: " + groupKey);
                
                AtomicInteger count = new AtomicInteger(0);
                AtomicInteger sumAge = new AtomicInteger(0);
                
                personStream.forEach(person -> {
                    count.incrementAndGet();
                    sumAge.addAndGet(person.getAge());
                    
                    // 各グループの最初の3人だけ表示
                    if (count.get() <= 3) {
                        System.out.println("     " + count.get() + ". " + 
                            person.getName() + " (" + person.getAge() + "歳) - " + 
                            person.getBirthplace());
                    }
                });
                
                double avgAge = count.get() > 0 ? (double) sumAge.get() / count.get() : 0;
                System.out.println("     ─────────────────────────");
                System.out.println("     👥 合計: " + count.get() + "人");
                System.out.println("     📊 平均年齢: " + String.format("%.1f", avgAge) + "歳");
            });
        
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("✅ 処理完了！");
        System.out.println("⏱️  処理時間: " + elapsed + "ms");
        System.out.println("💡 メモリ使用量: グループ数×100行程度（数MB）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
    }
    
    @Test
    @DisplayName("🔥 パターン2: インターフェース実装でグルーピング＆ソート")
    void pattern2_interfaceGroupingAndSorting() throws IOException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔥 パターン2: インターフェース実装 🔥");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        
        // テストデータ作成（10万行のCSV）
        Path csvPath = Paths.get("target/large_grouping_test2.csv");
        createTestCsv(csvPath, 100000);
        System.out.println("✅ テストCSV作成完了: " + csvPath);
        System.out.println();
        
        // GroupingSortableインターフェース実装を使った処理
        System.out.println("📊 グルーピング＆ソート処理開始...");
        System.out.println("💡 PersonWithGroupingがGroupingSortableを実装");
        System.out.println();
        long startTime = System.currentTimeMillis();
        
        LargeDataGroupingSorter.of(PersonWithGrouping.class, csvPath)
            .processGroupsSorted((groupKey, personStream) -> {
                System.out.println("  📁 グループ: " + groupKey);
                
                AtomicInteger count = new AtomicInteger(0);
                personStream.forEach(person -> {
                    count.incrementAndGet();
                    
                    // 各グループの最初の5人だけ表示
                    if (count.get() <= 5) {
                        System.out.println("     " + count.get() + ". " + 
                            person.getName() + " (" + person.getAge() + "歳)");
                    }
                });
                
                System.out.println("     👥 合計: " + count.get() + "人");
                System.out.println();
            });
        
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("✅ 処理完了！");
        System.out.println("⏱️  処理時間: " + elapsed + "ms");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
    }
    
    @Test
    @DisplayName("🔥 パターン3: 複雑なキーとソート条件")
    void pattern3_complexKeyAndSorting() throws IOException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔥 パターン3: 複雑なキー＆ソート 🔥");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        
        // テストデータ作成
        Path csvPath = Paths.get("target/large_grouping_test3.csv");
        createTestCsv(csvPath, 50000);
        System.out.println("✅ テストCSV作成完了: " + csvPath);
        System.out.println();
        
        // 複合キーとマルチソート
        System.out.println("📊 複合キー（職業_出身地）でグルーピング");
        System.out.println("📊 年齢降順 → 名前昇順でソート");
        System.out.println();
        long startTime = System.currentTimeMillis();
        
        LargeDataGroupingSorter.of(Person.class, csvPath)
            .groupBy(p -> p.getOccupation() + "_" + p.getBirthplace())  // 複合キー
            .sortBy(Comparator.comparingInt(Person::getAge).reversed()  // 年齢降順
                              .thenComparing(Person::getName))          // 名前昇順
            .processGroups((groupKey, personStream) -> {
                System.out.println("  📁 グループ: " + groupKey);
                
                // トップ3を取得
                AtomicInteger rank = new AtomicInteger(0);
                personStream.limit(3).forEach(person -> {
                    int r = rank.incrementAndGet();
                    System.out.println("     🏆 " + r + "位: " + 
                        person.getName() + " (" + person.getAge() + "歳)");
                });
                System.out.println();
            });
        
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("✅ 処理完了！");
        System.out.println("⏱️  処理時間: " + elapsed + "ms");
        System.out.println("💡 複合キー、マルチソート、トップN取得を実現！");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
    }
    
    @Test
    @DisplayName("🔥 パターン4: グループごとにファイル出力")
    void pattern4_outputToFiles() throws IOException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔥 パターン4: グループごとにファイル出力 🔥");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        
        // テストデータ作成
        Path csvPath = Paths.get("target/large_grouping_test4.csv");
        createTestCsv(csvPath, 50000);
        System.out.println("✅ テストCSV作成完了: " + csvPath);
        System.out.println();
        
        // グループごとに別ファイルに保存
        System.out.println("📊 職業ごとに別ファイルに出力...");
        long startTime = System.currentTimeMillis();
        
        Path outputDir = Paths.get("target/grouped_output");
        Files.createDirectories(outputDir);
        
        LargeDataGroupingSorter.of(Person.class, csvPath)
            .groupBy(Person::getOccupation)
            .sortBy(Comparator.comparingInt(Person::getAge))
            .processGroups((groupKey, personStream) -> {
                try {
                    Path outputFile = outputDir.resolve(groupKey + "_sorted.csv");
                    try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
                        // ヘッダー書き込み
                        writer.write("名前,年齢,職業,出身地");
                        writer.newLine();
                        
                        AtomicInteger count = new AtomicInteger(0);
                        personStream.forEach(person -> {
                            try {
                                writer.write(String.format("%s,%d,%s,%s",
                                    person.getName(),
                                    person.getAge(),
                                    person.getOccupation(),
                                    person.getBirthplace()));
                                writer.newLine();
                                count.incrementAndGet();
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
                        
                        System.out.println("  ✅ " + groupKey + ": " + count.get() + "件出力");
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("✅ 処理完了！");
        System.out.println("⏱️  処理時間: " + elapsed + "ms");
        System.out.println("📁 出力先: " + outputDir.toAbsolutePath());
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
    }
    
    /**
     * テスト用CSV作成
     */
    private void createTestCsv(Path csvPath, int rowCount) throws IOException {
        // 親ディレクトリが存在しない場合は作成
        Files.createDirectories(csvPath.getParent());
        
        String[] occupations = {"エンジニア", "デザイナー", "営業", "マネージャー", "企画"};
        String[] cities = {"東京", "大阪", "福岡", "名古屋", "札幌"};
        
        try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
            // ヘッダー行
            writer.write("名前,年齢,職業,出身地");
            writer.newLine();
            
            // データ行
            for (int i = 1; i <= rowCount; i++) {
                String name = "社員" + i;
                int age = 20 + (i % 40);
                String occupation = occupations[i % occupations.length];
                String city = cities[i % cities.length];
                
                writer.write(String.format("%s,%d,%s,%s", name, age, occupation, city));
                writer.newLine();
            }
        }
    }
}

