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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 大量データのExcelストリーミング処理デモ
 * メモリ効率の良い処理を実演
 */
public class LargeExcelStreamDemo {

    @AfterAll
    static void tearDown() throws IOException {
        // テスト終了時に生成されたファイルを削除
        Path largeExcel = Paths.get("target/large_data.xlsx");
        if (Files.exists(largeExcel)) {
            try {
                Files.delete(largeExcel);
            } catch (IOException e) {
                // 削除に失敗しても続行（ファイルがロックされている場合など）
            }
        }
    }

    @Test
    @DisplayName("🔥 Excel大量読み込みデモ - メモリ効率の良いストリーミング処理")
    void demonstrateLargeDataStreaming() throws IOException {
        System.out.println("🔥 Excel大量読み込みデモ開始！ 🔥");
        System.out.println();

        // 1. テスト用の大きなExcelファイルを作成
        Path largeExcel = Paths.get("target/large_data.xlsx");
        int rowCount = 10000; // 1万行のデータ
        
        System.out.println("📝 " + rowCount + "行のExcelファイルを作成中...");
        createLargeExcel(largeExcel, rowCount);
        System.out.println("✅ ファイル作成完了！");
        System.out.println();

        // 2. ストリーミング処理で読み込み
        System.out.println("📖 ストリーミング処理で読み込み開始...");
        long startTime = System.currentTimeMillis();
        
        // 全件数をカウント
        long totalCount = ExcelStreamReader.builder(Person.class, largeExcel)
            .extract(Stream::count);
        
        long endTime = System.currentTimeMillis();
        System.out.println("✅ 読み込み完了！ 件数: " + totalCount + "件");
        System.out.println("⏱️  処理時間: " + (endTime - startTime) + "ms");
        System.out.println();

        // 3. フィルタリング処理のデモ
        System.out.println("🔍 30歳以上の人をフィルタリング...");
        startTime = System.currentTimeMillis();
        
        List<Person> filtered = ExcelStreamReader.builder(Person.class, largeExcel)
            .extract(stream -> stream
                .filter(person -> person.getAge() >= 30)
                .limit(10)  // 最初の10件のみ取得
                .collect(Collectors.toList()));
        
        endTime = System.currentTimeMillis();
        System.out.println("✅ フィルタリング完了！ 該当件数（最初の10件）: " + filtered.size() + "件");
        System.out.println("⏱️  処理時間: " + (endTime - startTime) + "ms");
        System.out.println();

        // 4. 結果を表示
        System.out.println("📋 取得したデータ（最初の5件）:");
        filtered.stream().limit(5).forEach(person -> 
            System.out.println("  - " + person.getName() + " (" + person.getAge() + "歳) " + person.getOccupation())
        );
        System.out.println();

        // 5. マッピング処理のデモ
        System.out.println("🗺️  名前だけを抽出（最初の10件）...");
        startTime = System.currentTimeMillis();
        
        List<String> names = ExcelStreamReader.builder(Person.class, largeExcel)
            .extract(stream -> stream
                .map(Person::getName)
                .limit(10)
                .collect(Collectors.toList()));
        
        endTime = System.currentTimeMillis();
        System.out.println("✅ マッピング完了！ 件数: " + names.size() + "件");
        System.out.println("⏱️  処理時間: " + (endTime - startTime) + "ms");
        System.out.println("   → " + names);
        System.out.println();

        System.out.println("🎉 デモ完了！メモリ効率の良いストリーミング処理が動いたよ！ 🎉");
        System.out.println();
        System.out.println("💡 ポイント:");
        System.out.println("   - " + rowCount + "行のデータを処理しても、メモリは約100行分しか使わない！");
        System.out.println("   - Stream APIでフィルタリング、マッピングが自由自在！");
        System.out.println("   - 大量データでもサクサク処理できる！");
    }

    /**
     * テスト用の大きなExcelファイルを作成
     */
    private static void createLargeExcel(Path filePath, int rowCount) throws IOException {
        // 親ディレクトリが存在しない場合は作成
        Files.createDirectories(filePath.getParent());
        
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(filePath.toFile())) {

            Sheet sheet = workbook.createSheet("大量データ");

            // ヘッダー行を作成
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

