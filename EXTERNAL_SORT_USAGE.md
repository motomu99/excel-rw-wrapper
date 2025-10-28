# CSV外部ソート機能の使い方

## 概要

大容量（4GB～10GB程度）のCSVファイルをメモリに収まらない場合でも効率的にソートできる外部ソート機能を提供します。

## 主な特徴

- **メモリ効率**: ファイルをチャンクに分割してソートするため、メモリに収まりきらない大きなファイルでも処理可能
- **柔軟なソート条件**: Comparatorを使用して任意の列や複数列でのソートが可能
- **カスタマイズ可能**: チャンクサイズ、文字エンコーディング、ファイル形式などを指定可能
- **自動クリーンアップ**: 一時ファイルは処理後に自動的に削除

## 基本的な使い方

### 1. 単一列でのソート（文字列）

```java
import com.example.csv.CsvExternalSorter;
import java.nio.file.Paths;

public class Example1 {
    public static void main(String[] args) throws Exception {
        // name列でソート
        CsvExternalSorter.builder(
            Paths.get("large_input.csv"),
            Paths.get("sorted_output.csv")
        )
        .chunkSize(100_000_000)  // 100MBごとにチャンク分割
        .comparator((line1, line2) -> {
            String name1 = line1.split(",")[0];
            String name2 = line2.split(",")[0];
            return name1.compareTo(name2);
        })
        .sort();
        
        System.out.println("ソート完了！");
    }
}
```

### 2. 数値列でのソート

```java
import com.example.csv.CsvExternalSorter;
import java.nio.file.Paths;

public class Example2 {
    public static void main(String[] args) throws Exception {
        // id列（数値）でソート
        CsvExternalSorter.builder(
            Paths.get("employees.csv"),
            Paths.get("employees_sorted_by_id.csv")
        )
        .chunkSize(200_000_000)  // 200MB
        .comparator((line1, line2) -> {
            String[] cols1 = line1.split(",");
            String[] cols2 = line2.split(",");
            int id1 = Integer.parseInt(cols1[0]);
            int id2 = Integer.parseInt(cols2[0]);
            return Integer.compare(id1, id2);
        })
        .sort();
        
        System.out.println("ID順にソート完了！");
    }
}
```

### 3. 降順ソート

```java
import com.example.csv.CsvExternalSorter;
import java.nio.file.Paths;

public class Example3 {
    public static void main(String[] args) throws Exception {
        // score列で降順ソート
        CsvExternalSorter.builder(
            Paths.get("scores.csv"),
            Paths.get("scores_sorted_desc.csv")
        )
        .comparator((line1, line2) -> {
            String[] cols1 = line1.split(",");
            String[] cols2 = line2.split(",");
            int score1 = Integer.parseInt(cols1[2]);
            int score2 = Integer.parseInt(cols2[2]);
            return Integer.compare(score2, score1); // 逆順
        })
        .sort();
        
        System.out.println("スコア降順ソート完了！");
    }
}
```

### 4. 複数列でのソート

```java
import com.example.csv.CsvExternalSorter;
import java.nio.file.Paths;

public class Example4 {
    public static void main(String[] args) throws Exception {
        // department列 → name列の順でソート
        CsvExternalSorter.builder(
            Paths.get("employees.csv"),
            Paths.get("employees_sorted_by_dept_name.csv")
        )
        .comparator((line1, line2) -> {
            String[] cols1 = line1.split(",");
            String[] cols2 = line2.split(",");
            
            // まずdepartment列で比較
            int deptCompare = cols1[3].compareTo(cols2[3]);
            if (deptCompare != 0) {
                return deptCompare;
            }
            
            // departmentが同じならname列で比較
            return cols1[1].compareTo(cols2[1]);
        })
        .sort();
        
        System.out.println("部署・名前順にソート完了！");
    }
}
```

### 5. TSVファイルのソート

```java
import com.example.csv.CsvExternalSorter;
import com.example.csv.FileType;
import java.nio.file.Paths;

public class Example5 {
    public static void main(String[] args) throws Exception {
        // TSV（タブ区切り）ファイルをソート
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
}
```

### 6. Shift-JISエンコーディング

```java
import com.example.csv.CsvExternalSorter;
import com.example.csv.CharsetType;
import java.nio.file.Paths;

public class Example6 {
    public static void main(String[] args) throws Exception {
        // Shift-JISエンコーディングのCSVをソート
        CsvExternalSorter.builder(
            Paths.get("data_sjis.csv"),
            Paths.get("data_sorted_sjis.csv")
        )
        .charset(CharsetType.SHIFT_JIS)
        .comparator((line1, line2) -> {
            String name1 = line1.split(",")[0];
            String name2 = line2.split(",")[0];
            return name1.compareTo(name2);
        })
        .sort();
        
        System.out.println("Shift-JIS CSVソート完了！");
    }
}
```

### 7. ヘッダーなしのファイル

```java
import com.example.csv.CsvExternalSorter;
import java.nio.file.Paths;

public class Example7 {
    public static void main(String[] args) throws Exception {
        // ヘッダー行がないファイルをソート
        CsvExternalSorter.builder(
            Paths.get("no_header.csv"),
            Paths.get("no_header_sorted.csv")
        )
        .skipHeader(false)  // ヘッダーをスキップしない
        .comparator((line1, line2) -> {
            String value1 = line1.split(",")[0];
            String value2 = line2.split(",")[0];
            return value1.compareTo(value2);
        })
        .sort();
        
        System.out.println("ヘッダーなしCSVソート完了！");
    }
}
```

### 8. カスタム一時ディレクトリの指定

```java
import com.example.csv.CsvExternalSorter;
import java.nio.file.Paths;

public class Example8 {
    public static void main(String[] args) throws Exception {
        // 一時ファイルの保存先を指定
        CsvExternalSorter.builder(
            Paths.get("large_file.csv"),
            Paths.get("large_file_sorted.csv")
        )
        .tempDirectory(Paths.get("/mnt/large_disk/temp"))  // 大容量ディスクを指定
        .comparator(String::compareTo)
        .sort();
        
        System.out.println("ソート完了（カスタム一時ディレクトリ使用）！");
    }
}
```

## Builderメソッド一覧

| メソッド | 説明 | デフォルト値 |
|---------|------|-------------|
| `chunkSize(long)` | チャンクサイズをバイト単位で指定 | 100,000,000 (100MB) |
| `charset(CharsetType)` | 文字エンコーディングを指定 | UTF_8 |
| `fileType(FileType)` | ファイルタイプ（CSV/TSV）を指定 | CSV |
| `comparator(Comparator<String>)` | ソート条件を指定 | String::compareTo |
| `skipHeader(boolean)` | ヘッダー行をスキップするか | true |
| `tempDirectory(Path)` | 一時ファイルの保存先を指定 | システムのtempディレクトリ |
| `sort()` | ソートを実行 | - |

## パフォーマンスチューニング

### チャンクサイズの調整

- **小さすぎる場合**: チャンク数が増え、マージ処理に時間がかかる
- **大きすぎる場合**: メモリ不足になる可能性がある
- **推奨値**: 使用可能なメモリの30-50%程度

```java
// 例: 8GBメモリの場合、2-4GB程度
.chunkSize(3_000_000_000L)  // 3GB
```

### 一時ディレクトリの選択

- 高速なSSDディスクを使用すると処理速度が向上します
- 十分な空き容量があるディスクを指定してください（入力ファイルサイズの1.5倍以上推奨）

## エラーハンドリング

```java
import com.example.csv.CsvExternalSorter;
import java.nio.file.Paths;
import java.io.IOException;

public class ErrorHandlingExample {
    public static void main(String[] args) {
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
                    return line1.compareTo(line2);
                }
            })
            .sort();
            
            System.out.println("ソート成功！");
            
        } catch (IOException e) {
            System.err.println("ファイル操作エラー: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("予期しないエラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## 注意事項

1. **メモリ使用量**: チャンクサイズはJVMのヒープサイズを考慮して設定してください
2. **ディスク容量**: 一時ファイル用に入力ファイルサイズの1.5倍以上の空き容量が必要です
3. **文字エンコーディング**: 入力ファイルと出力ファイルは同じエンコーディングになります
4. **ヘッダー行**: デフォルトでは1行目をヘッダーとして扱い、ソート対象から除外します
5. **引用符**: CSV内の引用符やカンマを含むフィールドは適切にエスケープされている必要があります

## 処理フロー

1. **チャンク分割**: 入力ファイルをチャンクサイズごとに読み込み
2. **チャンクソート**: 各チャンクをメモリ内でソート
3. **一時保存**: ソート済みチャンクを一時ファイルに保存
4. **k-wayマージ**: すべての一時ファイルをマージソートで結合
5. **クリーンアップ**: 一時ファイルを自動削除

## 技術詳細

- **アルゴリズム**: 外部マージソート（External Merge Sort）
- **マージ方式**: k-wayマージ（Priority Queueを使用）
- **一時ファイル**: UUID付きディレクトリで管理し、処理後に自動削除

## ライセンス

このプロジェクトのライセンスに従います。
