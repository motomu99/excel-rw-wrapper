package com.example.csv;

import java.nio.file.Paths;

/**
 * 外部ソート機能のサンプルコード
 * 
 * このクラスは、CsvExternalSorterの使用例を示すサンプルです。
 * 実際に実行する場合は、適切なCSVファイルを用意してください。
 */
public class ExternalSortExample {
    
    /**
     * 基本的な使用例
     */
    public static void example1_BasicSort() throws Exception {
        System.out.println("=== 例1: 基本的なソート ===");
        
        CsvExternalSorter.builder(
            Paths.get("input.csv"),
            Paths.get("output_sorted.csv")
        )
        .chunkSize(100_000_000)  // 100MB
        .comparator((line1, line2) -> {
            // 1列目（name）でソート
            String name1 = line1.split(",")[0];
            String name2 = line2.split(",")[0];
            return name1.compareTo(name2);
        })
        .sort();
        
        System.out.println("ソート完了！");
    }
    
    /**
     * 数値列でのソート例
     */
    public static void example2_NumericSort() throws Exception {
        System.out.println("=== 例2: 数値列でのソート ===");
        
        CsvExternalSorter.builder(
            Paths.get("employees.csv"),
            Paths.get("employees_sorted_by_id.csv")
        )
        .chunkSize(200_000_000)  // 200MB
        .comparator((line1, line2) -> {
            // ID列（数値）でソート
            String[] cols1 = line1.split(",");
            String[] cols2 = line2.split(",");
            int id1 = Integer.parseInt(cols1[0]);
            int id2 = Integer.parseInt(cols2[0]);
            return Integer.compare(id1, id2);
        })
        .sort();
        
        System.out.println("ID順にソート完了！");
    }
    
    /**
     * 降順ソートの例
     */
    public static void example3_DescendingSort() throws Exception {
        System.out.println("=== 例3: 降順ソート ===");
        
        CsvExternalSorter.builder(
            Paths.get("scores.csv"),
            Paths.get("scores_sorted_desc.csv")
        )
        .comparator((line1, line2) -> {
            // Score列で降順ソート
            String[] cols1 = line1.split(",");
            String[] cols2 = line2.split(",");
            int score1 = Integer.parseInt(cols1[1]);
            int score2 = Integer.parseInt(cols2[1]);
            return Integer.compare(score2, score1); // 降順
        })
        .sort();
        
        System.out.println("スコア降順ソート完了！");
    }
    
    /**
     * 複数列でのソート例
     */
    public static void example4_MultiColumnSort() throws Exception {
        System.out.println("=== 例4: 複数列でのソート ===");
        
        CsvExternalSorter.builder(
            Paths.get("employees.csv"),
            Paths.get("employees_sorted_by_dept_name.csv")
        )
        .comparator((line1, line2) -> {
            String[] cols1 = line1.split(",");
            String[] cols2 = line2.split(",");
            
            // まずDepartment列で比較
            int deptCompare = cols1[2].compareTo(cols2[2]);
            if (deptCompare != 0) {
                return deptCompare;
            }
            
            // Departmentが同じならName列で比較
            return cols1[1].compareTo(cols2[1]);
        })
        .sort();
        
        System.out.println("部署・名前順にソート完了！");
    }
    
    /**
     * TSVファイルのソート例
     */
    public static void example5_TsvSort() throws Exception {
        System.out.println("=== 例5: TSVファイルのソート ===");
        
        CsvExternalSorter.builder(
            Paths.get("data.tsv"),
            Paths.get("data_sorted.tsv")
        )
        .fileType(FileType.TSV)
        .comparator((line1, line2) -> {
            String name1 = line1.split("\t")[0];
            String name2 = line2.split("\t")[0];
            return name1.compareTo(name2);
        })
        .sort();
        
        System.out.println("TSVソート完了！");
    }
    
    /**
     * Shift-JISエンコーディングの例
     */
    public static void example6_ShiftJisSort() throws Exception {
        System.out.println("=== 例6: Shift-JISエンコーディング ===");
        
        CsvExternalSorter.builder(
            Paths.get("data_sjis.csv"),
            Paths.get("data_sorted_sjis.csv")
        )
        .charset(CharsetType.S_JIS)
        .comparator((line1, line2) -> {
            String name1 = line1.split(",")[0];
            String name2 = line2.split(",")[0];
            return name1.compareTo(name2);
        })
        .sort();
        
        System.out.println("Shift-JIS CSVソート完了！");
    }
    
    /**
     * エラーハンドリングの例
     */
    public static void example7_ErrorHandling() {
        System.out.println("=== 例7: エラーハンドリング ===");
        
        try {
            CsvExternalSorter.builder(
                Paths.get("input.csv"),
                Paths.get("output.csv")
            )
            .comparator((line1, line2) -> {
                try {
                    String[] cols1 = line1.split(",");
                    String[] cols2 = line2.split(",");
                    return Integer.compare(
                        Integer.parseInt(cols1[0]),
                        Integer.parseInt(cols2[0])
                    );
                } catch (NumberFormatException e) {
                    // パースエラーの場合は文字列として比較
                    System.err.println("数値変換エラー、文字列比較にフォールバック: " + e.getMessage());
                    return line1.compareTo(line2);
                }
            })
            .sort();
            
            System.out.println("ソート成功！");
            
        } catch (Exception e) {
            System.err.println("エラーが発生しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 大容量ファイル向けの設定例
     */
    public static void example8_LargeFileConfiguration() throws Exception {
        System.out.println("=== 例8: 大容量ファイル向け設定 ===");
        
        CsvExternalSorter.builder(
            Paths.get("very_large_file.csv"),  // 4-10GB
            Paths.get("very_large_file_sorted.csv")
        )
        .chunkSize(500_000_000L)  // 500MB（メモリに合わせて調整）
        .tempDirectory(Paths.get("/mnt/ssd/temp"))  // 高速ディスクを指定
        .charset(CharsetType.UTF_8)
        .comparator((line1, line2) -> {
            String[] cols1 = line1.split(",");
            String[] cols2 = line2.split(",");
            
            // 複数条件でソート
            // 1. まず日付列でソート
            int dateCompare = cols1[3].compareTo(cols2[3]);
            if (dateCompare != 0) return dateCompare;
            
            // 2. 次にID列でソート
            return Integer.compare(
                Integer.parseInt(cols1[0]),
                Integer.parseInt(cols2[0])
            );
        })
        .sort();
        
        System.out.println("大容量ファイルのソート完了！");
    }
    
    /**
     * メイン実行メソッド（サンプル実行用）
     */
    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println("  CSV外部ソート サンプルコード集");
        System.out.println("==============================================\n");
        
        // 注意: このサンプルを実行するには、適切なCSVファイルを用意してください
        System.out.println("注意: 各サンプルを実行するには、対応するCSVファイルを用意してください。");
        System.out.println("詳細は EXTERNAL_SORT_USAGE.md を参照してください。\n");
        
        // 必要に応じてコメントを外してサンプルを実行
        // try {
        //     example1_BasicSort();
        //     example2_NumericSort();
        //     example3_DescendingSort();
        //     example4_MultiColumnSort();
        //     example5_TsvSort();
        //     example6_ShiftJisSort();
        //     example7_ErrorHandling();
        //     example8_LargeFileConfiguration();
        // } catch (Exception e) {
        //     e.printStackTrace();
        // }
    }
}
