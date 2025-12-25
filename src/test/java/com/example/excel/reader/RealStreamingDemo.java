package com.example.excel.reader;

import com.example.excel.reader.ExcelStreamReader;
import com.example.model.Person;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * 🔥 真のストリーミング処理デモ 🔥
 * メモリに全件載せないでExcelを処理する方法
 */
public class RealStreamingDemo {

    @AfterAll
    static void tearDown() throws IOException {
        // テスト終了時に生成されたファイルを削除
        Path hugeExcel = Paths.get("target/huge_data.xlsx");
        if (Files.exists(hugeExcel)) {
            try {
                Files.delete(hugeExcel);
            } catch (IOException e) {
                // 削除に失敗しても続行（ファイルがロックされている場合など）
            }
        }
    }

    @Test
    @DisplayName("🚀 正しいストリーミング処理 - メモリに全件載せない！")
    void demonstrateRealStreaming() throws IOException {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🔥 真のストリーミング処理デモ開始！ 🔥");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();

        // テスト用の大量Excelファイルを作成（10万行）
        Path largeExcel = Paths.get("target/huge_data.xlsx");
        int rowCount = 100000;
        
        System.out.println("📝 " + rowCount + "行のExcelファイルを作成中...");
        createHugeExcel(largeExcel, rowCount);
        System.out.println("✅ ファイル作成完了！");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ❌ NG例：全件メモリに載せる（メモリやばい）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("❌ NG例：List<Bean>に全件格納（メモリ大量消費）");
        System.out.println("   ⚠️  これはやっちゃダメ！");
        System.out.println();
        System.out.println("   List<Person> allData = ExcelStreamReader.builder(Person.class, path)");
        System.out.println("       .extract(stream -> stream.collect(Collectors.toList()));");
        System.out.println("   // ↑ 10万件全部メモリに載る！メモリ不足で死ぬ！");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ✅ OK例1：1件ずつDB保存（メモリ最小）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("✅ OK例1：1件ずつDB保存（メモリ常に1件分のみ）");
        long startTime = System.currentTimeMillis();
        
        AtomicInteger savedCount = new AtomicInteger(0);
        ExcelStreamReader.builder(Person.class, largeExcel)
            .consume(stream -> {
                stream.forEach(person -> {
                    // ここで1件ずつDB保存（実際のコード例）
                    // personRepository.save(person);
                    // または
                    // jdbcTemplate.update("INSERT INTO ...", person.getName(), ...);
                    
                    savedCount.incrementAndGet();
                    
                    // 進捗表示（1万件ごと）
                    if (savedCount.get() % 10000 == 0) {
                        System.out.println("   💾 保存完了: " + savedCount.get() + "件");
                    }
                });
            });
        
        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("   ✅ 全件保存完了！ 件数: " + savedCount.get() + "件");
        System.out.println("   ⏱️  処理時間: " + elapsed + "ms");
        System.out.println("   💡 メモリ使用量: 常に100行分程度（約数MB）");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ✅ OK例2：バッチ処理（100件ずつまとめて保存）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("✅ OK例2：バッチ処理（100件ごとにまとめてDB保存）");
        startTime = System.currentTimeMillis();
        
        AtomicInteger batchCount = new AtomicInteger(0);
        List<Person> batch = new ArrayList<>();
        final int BATCH_SIZE = 100;
        
        ExcelStreamReader.builder(Person.class, largeExcel)
            .consume(stream -> {
                stream.forEach(person -> {
                    batch.add(person);
                    
                    // 100件たまったらまとめて保存
                    if (batch.size() >= BATCH_SIZE) {
                        // バッチ保存（実際のコード例）
                        // personRepository.saveAll(batch);
                        // または
                        // jdbcTemplate.batchUpdate("INSERT INTO ...", batch);
                        
                        batchCount.addAndGet(batch.size());
                        batch.clear(); // メモリ解放！
                        
                        if (batchCount.get() % 10000 == 0) {
                            System.out.println("   💾 バッチ保存完了: " + batchCount.get() + "件");
                        }
                    }
                });
                
                // 残りを保存
                if (!batch.isEmpty()) {
                    batchCount.addAndGet(batch.size());
                    batch.clear();
                }
            });
        
        elapsed = System.currentTimeMillis() - startTime;
        System.out.println("   ✅ バッチ保存完了！ 件数: " + batchCount.get() + "件");
        System.out.println("   ⏱️  処理時間: " + elapsed + "ms");
        System.out.println("   💡 メモリ使用量: 常に100行分（バッチサイズ分）");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ✅ OK例3：集計処理（メモリに載せない）
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("✅ OK例3：集計処理（全件メモリに載せずに集計）");
        startTime = System.currentTimeMillis();
        
        // カウント
        long totalCount = ExcelStreamReader.builder(Person.class, largeExcel)
            .extract(Stream::count);
        
        // 平均年齢
        double averageAge = ExcelStreamReader.builder(Person.class, largeExcel)
            .extract(stream -> stream
                .mapToInt(Person::getAge)
                .average()
                .orElse(0.0));
        
        // 最高年齢
        int maxAge = ExcelStreamReader.builder(Person.class, largeExcel)
            .extract(stream -> stream
                .mapToInt(Person::getAge)
                .max()
                .orElse(0));
        
        elapsed = System.currentTimeMillis() - startTime;
        System.out.println("   ✅ 集計完了！");
        System.out.println("      - 総件数: " + totalCount + "件");
        System.out.println("      - 平均年齢: " + String.format("%.1f", averageAge) + "歳");
        System.out.println("      - 最高年齢: " + maxAge + "歳");
        System.out.println("   ⏱️  処理時間: " + elapsed + "ms");
        System.out.println("   💡 メモリ使用量: 常に100行分のみ（集計値のみ保持）");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // ✅ OK例4：条件付きで早期終了
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("✅ OK例4：条件付き早期終了（最初の100件だけ処理）");
        startTime = System.currentTimeMillis();
        
        AtomicInteger processedCount = new AtomicInteger(0);
        ExcelStreamReader.builder(Person.class, largeExcel)
            .consume(stream -> {
                stream
                    .limit(100)  // 最初の100件だけ！
                    .forEach(person -> {
                        // 処理
                        processedCount.incrementAndGet();
                    });
            });
        
        elapsed = System.currentTimeMillis() - startTime;
        System.out.println("   ✅ 処理完了！ 件数: " + processedCount.get() + "件");
        System.out.println("   ⏱️  処理時間: " + elapsed + "ms");
        System.out.println("   💡 残りの" + (rowCount - 100) + "件は読み込まずにスキップ！");
        System.out.println();

        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        // まとめ
        // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("🎉 まとめ：メモリに優しい処理方法 🎉");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println();
        System.out.println("✅ やるべきこと:");
        System.out.println("   1. forEach で1件ずつ処理（DB保存など）");
        System.out.println("   2. バッチ処理（100-1000件ごとにまとめて保存）");
        System.out.println("   3. 集計処理（count, sum, average, max, min）");
        System.out.println("   4. limit で必要な件数だけ処理");
        System.out.println();
        System.out.println("❌ やっちゃダメなこと:");
        System.out.println("   1. collect(Collectors.toList()) で全件取得");
        System.out.println("   2. 全件をListやMapに格納");
        System.out.println();
        System.out.println("💡 ポイント:");
        System.out.println("   - ストリーミング = メモリに全件載せないこと！");
        System.out.println("   - 常に少量のデータのみメモリに保持！");
        System.out.println("   - " + rowCount + "行でもメモリは数MB程度！");
        System.out.println();
    }

    /**
     * テスト用の大量Excelファイルを作成
     */
    private void createHugeExcel(Path filePath, int rowCount) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath.toFile())) {

            Sheet sheet = workbook.createSheet("大量データ");

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
}

