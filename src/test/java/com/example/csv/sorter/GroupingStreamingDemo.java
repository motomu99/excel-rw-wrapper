package com.example.csv.sorter;

import com.example.excel.reader.ExcelStreamReader;
import com.example.model.Person;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 🔥 グルーピング処理のベストプラクティス 🔥
 * メモリに優しいグルーピング方法を紹介
 */
public class GroupingStreamingDemo {

    @Test
    @DisplayName("📊 グルーピング処理の正しい方法")
    void demonstrateGroupingStrategies() throws IOException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔥 グルーピング処理デモ開始！ 🔥");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        // テスト用Excel作成（10万行）
        Path largeExcel = Paths.get("target/grouping_data.xlsx");
        int rowCount = 100000;
        
        System.out.println("📝 " + rowCount + "行のExcelファイルを作成中...");
        createGroupingTestExcel(largeExcel, rowCount);
        System.out.println("✅ ファイル作成完了！");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ❌ NG例：全件グルーピング（メモリやばい）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("❌ NG例：Collectors.groupingBy() で全件グルーピング");
        System.out.println("   ⚠️  これはやっちゃダメ！全件メモリに載る！");
        System.out.println();
        System.out.println("   Map<String, List<Person>> grouped = stream");
        System.out.println("       .collect(Collectors.groupingBy(Person::getOccupation));");
        System.out.println("   // ↑ 10万件全部メモリに載る！");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ✅ OK例1：グループごとの集計のみ（メモリ最小）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("✅ OK例1：グループごとの集計のみ保持（実データは保持しない）");
        long startTime = System.currentTimeMillis();
        
        // 職業ごとの人数と平均年齢
        Map<String, OccupationStats> statsMap = new ConcurrentHashMap<>();
        
        ExcelStreamReader.of(Person.class, largeExcel)
            .process(stream -> {
                stream.forEach(person -> {
                    String occupation = person.getOccupation();
                    statsMap.computeIfAbsent(occupation, k -> new OccupationStats())
                           .add(person.getAge());
                });
                return null;
            });
        
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("   ✅ 集計完了！");
        System.out.println();
        statsMap.forEach((occupation, stats) -> {
            System.out.println("      📊 " + occupation + ":");
            System.out.println("         人数: " + stats.getCount() + "人");
            System.out.println("         平均年齢: " + String.format("%.1f", stats.getAverage()) + "歳");
        });
        System.out.println();
        System.out.println("   ⏱️  処理時間: " + elapsed + "ms");
        System.out.println("   💡 メモリ使用量: グループ数×集計値のみ（数KB）");
        System.out.println("   💡 10万件の実データは保持していない！");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ✅ OK例2：グループごとに直接DB保存（バッチ処理）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("✅ OK例2：グループごとにバッチ保存（メモリは各グループのバッチサイズ分のみ）");
        startTime = System.currentTimeMillis();
        
        final int BATCH_SIZE = 100;
        Map<String, List<Person>> batchMap = new HashMap<>();
        Map<String, Integer> saveCount = new HashMap<>();
        
        ExcelStreamReader.of(Person.class, largeExcel)
            .process(stream -> {
                stream.forEach(person -> {
                    String occupation = person.getOccupation();
                    
                    // 職業ごとのバッチに追加
                    batchMap.computeIfAbsent(occupation, k -> new ArrayList<>())
                           .add(person);
                    
                    // バッチサイズに達したら保存
                    List<Person> batch = batchMap.get(occupation);
                    if (batch.size() >= BATCH_SIZE) {
                        // DB保存（例）
                        // personRepository.saveAll(batch);
                        saveCount.merge(occupation, batch.size(), Integer::sum);
                        batch.clear();  // ⭐ メモリ解放！
                    }
                });
                
                // 残りを保存
                batchMap.forEach((occupation, batch) -> {
                    if (!batch.isEmpty()) {
                        saveCount.merge(occupation, batch.size(), Integer::sum);
                        batch.clear();
                    }
                });
                
                return null;
            });
        
        elapsed = System.currentTimeMillis() - startTime;
        System.out.println("   ✅ グループ別保存完了！");
        System.out.println();
        saveCount.forEach((occupation, count) -> {
            System.out.println("      💾 " + occupation + ": " + count + "件保存");
        });
        System.out.println();
        System.out.println("   ⏱️  処理時間: " + elapsed + "ms");
        System.out.println("   💡 メモリ使用量: グループ数×バッチサイズ（数MB）");
        System.out.println("   💡 例：5グループ×100件 = 500件分だけメモリに！");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ✅ OK例3：グループが少ない場合のみ全件保持を許容
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("✅ OK例3：グループ数が少ない場合（10個以下）は全件保持もあり");
        System.out.println("   ⚠️  ただし、各グループのデータ量に注意！");
        startTime = System.currentTimeMillis();
        
        // 出身地ごとのトップ10を取得
        Map<String, TopNCollector> topNMap = new HashMap<>();
        final int TOP_N = 10;
        
        ExcelStreamReader.of(Person.class, largeExcel)
            .process(stream -> {
                stream.forEach(person -> {
                    String city = person.getBirthplace();
                    topNMap.computeIfAbsent(city, k -> new TopNCollector(TOP_N))
                          .add(person);
                });
                return null;
            });
        
        elapsed = System.currentTimeMillis() - startTime;
        System.out.println("   ✅ トップN取得完了！");
        System.out.println();
        topNMap.forEach((city, topN) -> {
            System.out.println("      🏆 " + city + "（年齢トップ3）:");
            topN.getTop().stream().limit(3).forEach(person -> {
                System.out.println("         - " + person.getName() + " (" + person.getAge() + "歳)");
            });
        });
        System.out.println();
        System.out.println("   ⏱️  処理時間: " + elapsed + "ms");
        System.out.println("   💡 メモリ使用量: グループ数×トップN件（数十KB）");
        System.out.println("   💡 例：5グループ×10件 = 50件分だけ！");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ✅ OK例4：条件付きフィルタリング＋グループごと処理
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("✅ OK例4：条件でフィルタしてからグループ処理");
        startTime = System.currentTimeMillis();
        
        Map<String, AtomicInteger> seniorCount = new HashMap<>();
        
        ExcelStreamReader.of(Person.class, largeExcel)
            .process(stream -> {
                stream
                    .filter(person -> person.getAge() >= 50)  // 50歳以上のみ
                    .forEach(person -> {
                        seniorCount.computeIfAbsent(
                            person.getOccupation(), 
                            k -> new AtomicInteger()
                        ).incrementAndGet();
                    });
                return null;
            });
        
        elapsed = System.currentTimeMillis() - startTime;
        System.out.println("   ✅ フィルタリング＋集計完了！");
        System.out.println();
        seniorCount.forEach((occupation, count) -> {
            System.out.println("      👴 " + occupation + "（50歳以上）: " + count.get() + "人");
        });
        System.out.println();
        System.out.println("   ⏱️  処理時間: " + elapsed + "ms");
        System.out.println("   💡 メモリ使用量: グループ数×カウンタのみ（数バイト）");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // まとめ
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🎉 グルーピング処理のまとめ 🎉");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("✅ メモリに優しい方法:");
        System.out.println("   1. 集計値のみ保持（件数、合計、平均など）");
        System.out.println("   2. グループごとにバッチ保存");
        System.out.println("   3. トップNのみ保持（全件は持たない）");
        System.out.println("   4. フィルタリングしてデータ量削減");
        System.out.println();
        System.out.println("❌ やっちゃダメ:");
        System.out.println("   1. Collectors.groupingBy() で全件グルーピング");
        System.out.println("   2. Map<String, List<Person>> で全件保持");
        System.out.println();
        System.out.println("💡 判断基準:");
        System.out.println("   - グループ数が少ない（10個以下）→ 部分的な保持OK");
        System.out.println("   - グループ数が多い → 集計のみ or バッチ処理");
        System.out.println("   - 各グループのデータ量が多い → 絶対にバッチ処理");
        System.out.println();
    }

    /**
     * テスト用Excel作成（グルーピング用）
     */
    private void createGroupingTestExcel(Path filePath, int rowCount) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath.toFile())) {

            Sheet sheet = workbook.createSheet("グルーピングテスト");

            // ヘッダー行
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("名前");
            headerRow.createCell(1).setCellValue("年齢");
            headerRow.createCell(2).setCellValue("職業");
            headerRow.createCell(3).setCellValue("出身地");

            // データ行を大量に作成
            String[] occupations = {"エンジニア", "デザイナー", "営業", "マネージャー", "企画"};
            String[] cities = {"東京", "大阪", "福岡", "名古屋", "札幌"};

            for (int i = 1; i <= rowCount; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue("社員" + i);
                row.createCell(1).setCellValue(20 + (i % 40)); // 20-59歳
                row.createCell(2).setCellValue(occupations[i % occupations.length]);
                row.createCell(3).setCellValue(cities[i % cities.length]);
            }

            workbook.write(fos);
        }
    }

    /**
     * グループごとの統計情報を保持するクラス
     */
    static class OccupationStats {
        private int count = 0;
        private long sum = 0;
        
        public void add(int age) {
            count++;
            sum += age;
        }
        
        public int getCount() {
            return count;
        }
        
        public double getAverage() {
            return count == 0 ? 0 : (double) sum / count;
        }
    }

    /**
     * トップN件のみを保持するコレクター
     * ソートされた状態で最大N件だけ保持
     */
    static class TopNCollector {
        private final int maxSize;
        private final PriorityQueue<Person> queue;
        
        public TopNCollector(int maxSize) {
            this.maxSize = maxSize;
            // 年齢の昇順（最小値が先頭）
            this.queue = new PriorityQueue<>(Comparator.comparingInt(Person::getAge));
        }
        
        public void add(Person person) {
            queue.offer(person);
            if (queue.size() > maxSize) {
                queue.poll();  // 最小値を削除
            }
        }
        
        public List<Person> getTop() {
            List<Person> result = new ArrayList<>(queue);
            // 年齢の降順にソート
            result.sort(Comparator.comparingInt(Person::getAge).reversed());
            return result;
        }
    }
}

