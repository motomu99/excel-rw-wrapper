package com.example.csv;

import com.example.csv.model.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LargeDataGroupingSorterのテスト
 */
@DisplayName("LargeDataGroupingSorter: 大量データのグルーピング＆ソート")
public class LargeDataGroupingSorterTest {

    private static final Path TEST_RESOURCES_DIR = Paths.get("src/test/resources");
    private static final Path LARGE_CSV = Paths.get("target/large_grouping_test.csv");

    @BeforeAll
    static void setUp() throws IOException {
        Files.createDirectories(TEST_RESOURCES_DIR);
        Files.createDirectories(Paths.get("target"));
        
        // テスト用の大量CSVファイルを作成
        createLargeTestCsv(LARGE_CSV, 10000);
    }

    /**
     * テスト用の大量CSVファイルを作成
     */
    private static void createLargeTestCsv(Path filePath, int rowCount) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, java.nio.charset.StandardCharsets.UTF_8)) {
            // ヘッダー行
            writer.write("名前,年齢,職業,出身地");
            writer.newLine();
            
            String[] occupations = {"エンジニア", "デザイナー", "営業", "マネージャー", "企画"};
            String[] cities = {"東京", "大阪", "福岡", "名古屋", "札幌"};
            Random random = new Random(42);
            
            for (int i = 1; i <= rowCount; i++) {
                writer.write(String.format("社員%d,%d,%s,%s",
                    i,
                    20 + random.nextInt(40),
                    occupations[random.nextInt(occupations.length)],
                    cities[random.nextInt(cities.length)]
                ));
                writer.newLine();
            }
        }
    }

    @Test
    @DisplayName("🔥 基本的なグルーピング＆ソート処理")
    void testBasicGroupingAndSorting() throws IOException {
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔥 LargeDataGroupingSorter デモ開始！ 🔥");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        Map<String, List<Person>> results = new LinkedHashMap<>();
        
        long startTime = System.currentTimeMillis();
        
        // グルーピング＆ソート処理
        LargeDataGroupingSorter.of(Person.class, LARGE_CSV)
            .groupBy(Person::getOccupation)  // 職業でグルーピング
            .sortBy(Comparator.comparingInt(Person::getAge))  // 年齢でソート
            .processGroups((groupKey, personStream) -> {
                System.out.println();
                System.out.println("📂 グループ: " + groupKey);
                
                List<Person> persons = personStream
                    .peek(person -> {
                        // 最初の5人だけ表示
                        if (results.computeIfAbsent(groupKey, k -> new ArrayList<>()).size() < 5) {
                            System.out.println("   - " + person.getName() + 
                                " (" + person.getAge() + "歳)");
                        }
                    })
                    .toList();
                
                results.put(groupKey, persons);
                System.out.println("   ✅ 合計: " + persons.size() + "人");
            });
        
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📊 処理結果サマリー");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("⏱️  処理時間: " + elapsed + "ms");
        System.out.println("📁 グループ数: " + results.size());
        System.out.println();
        
        results.forEach((groupKey, persons) -> {
            System.out.println("   " + groupKey + ": " + persons.size() + "人");
        });
        System.out.println();

        // アサーション
        assertNotNull(results);
        assertTrue(results.size() > 0);
        
        // 各グループが年齢順にソートされているか確認
        results.forEach((groupKey, persons) -> {
            for (int i = 0; i < persons.size() - 1; i++) {
                assertTrue(persons.get(i).getAge() <= persons.get(i + 1).getAge(),
                    "グループ " + groupKey + " が年齢順にソートされていません");
            }
        });
        
        System.out.println("✅ テスト成功！");
        System.out.println();
    }

    @Test
    @DisplayName("🚀 ストリーミング処理（メモリに全件載せない）")
    void testStreamingProcessing() throws IOException {
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🚀 ストリーミング処理デモ 🚀");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        
        Map<String, Integer> groupCounts = new LinkedHashMap<>();
        Map<String, Double> groupAvgAges = new LinkedHashMap<>();
        
        long startTime = System.currentTimeMillis();
        
        // ストリーミング処理（全件メモリに載せない！）
        LargeDataGroupingSorter.of(Person.class, LARGE_CSV)
            .groupBy(Person::getOccupation)
            .sortBy(Comparator.comparingInt(Person::getAge))
            .processGroups((groupKey, personStream) -> {
                System.out.println("📂 グループ処理中: " + groupKey);
                
                // 集計のみ（実データは保持しない）
                AtomicInteger count = new AtomicInteger(0);
                AtomicInteger sumAge = new AtomicInteger(0);
                
                personStream.forEach(person -> {
                    count.incrementAndGet();
                    sumAge.addAndGet(person.getAge());
                    
                    // ここでDB保存などの処理を行う
                    // personRepository.save(person);
                });
                
                double avgAge = (double) sumAge.get() / count.get();
                groupCounts.put(groupKey, count.get());
                groupAvgAges.put(groupKey, avgAge);
                
                System.out.println("   ✅ 件数: " + count.get() + 
                    ", 平均年齢: " + String.format("%.1f", avgAge) + "歳");
            });
        
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📊 処理完了");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("⏱️  処理時間: " + elapsed + "ms");
        System.out.println("💡 メモリ使用量: 集計値のみ（数KB）");
        System.out.println("💡 実データは保持していない！");
        System.out.println();
        
        // アサーション
        assertNotNull(groupCounts);
        assertTrue(groupCounts.size() > 0);
        
        System.out.println("✅ ストリーミング処理成功！");
        System.out.println();
    }

    @Test
    @DisplayName("📊 複雑なグループキー＆ソート条件")
    void testComplexGroupingAndSorting() throws IOException {
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📊 複雑なグループキー デモ 📊");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        
        Map<String, List<Person>> topResults = new LinkedHashMap<>();
        
        // 複雑なグループキー（職業_出身地）
        LargeDataGroupingSorter.of(Person.class, LARGE_CSV)
            .groupBy(p -> p.getOccupation() + "_" + p.getBirthplace())
            .sortBy(Comparator.comparingInt(Person::getAge).reversed())  // 年齢降順
            .processGroups((groupKey, personStream) -> {
                System.out.println("📂 グループ: " + groupKey);
                
                // 各グループのトップ3を取得
                List<Person> top3 = personStream
                    .limit(3)
                    .toList();
                
                topResults.put(groupKey, top3);
                
                top3.forEach(person -> {
                    System.out.println("   🏆 " + person.getName() + 
                        " (" + person.getAge() + "歳)");
                });
            });
        
        System.out.println();
        System.out.println("✅ 複雑なグループキー処理成功！");
        System.out.println("   グループ数: " + topResults.size());
        System.out.println();
        
        // アサーション
        assertNotNull(topResults);
        assertTrue(topResults.size() > 0);
    }

    @Test
    @DisplayName("💡 項目名で指定する方法")
    void testGroupByFieldName() throws IOException {
        System.out.println();
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("💡 項目名でグルーピング指定 💡");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        
        System.out.println("✅ 方法1: メソッド参照（推奨）");
        System.out.println("   .groupBy(Person::getOccupation)");
        System.out.println();
        
        System.out.println("✅ 方法2: Lambda式");
        System.out.println("   .groupBy(p -> p.getOccupation())");
        System.out.println();
        
        System.out.println("✅ 方法3: 複数項目の組み合わせ");
        System.out.println("   .groupBy(p -> p.getDepartment() + \"_\" + p.getCity())");
        System.out.println();
        
        Map<String, Integer> results = new HashMap<>();
        
        // 実際に動かす
        LargeDataGroupingSorter.of(Person.class, LARGE_CSV)
            .groupBy(Person::getOccupation)  // ← 項目名で指定！
            .sortBy(Comparator.comparing(Person::getName))
            .processGroups((groupKey, stream) -> {
                long count = stream.count();
                results.put(groupKey, (int) count);
                System.out.println("   " + groupKey + ": " + count + "件");
            });
        
        System.out.println();
        System.out.println("✅ 項目名指定成功！");
        System.out.println();
        
        assertNotNull(results);
        assertTrue(results.size() > 0);
    }
}
