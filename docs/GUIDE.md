# 📚 Excel RW Wrapper 完全ガイド

このドキュメントは、Excel RW Wrapperライブラリの全機能とベストプラクティスをまとめた統合ガイドです。

## 📋 目次

1. [基本機能](#基本機能)
2. [ストリーミング処理のベストプラクティス](#ストリーミング処理のベストプラクティス)
3. [グルーピング処理のベストプラクティス](#グルーピング処理のベストプラクティス)
4. [大量データ処理](#大量データ処理)
5. [DDD設計によるExcel書き込み](#ddd設計によるexcel書き込み)
6. [移行ガイド](#移行ガイド)

---

## 基本機能

### CSV読み込み

#### CsvReaderWrapper（推奨）

**新しいBuilderパターンを使用した、最も推奨される方法です。**

```java
import com.example.csv.CsvReaderWrapper;
import com.example.csv.model.Person;
import java.nio.file.Paths;
import java.util.List;

// シンプルな読み込み
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("path/to/your/file.csv"))
    .read();

// 詳細設定
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("data.tsv"))
    .charset(CharsetType.S_JIS)       // 文字セット指定
    .fileType(FileType.TSV)            // TSVファイル
    .skipLines(1)                      // 最初の1行をスキップ
    .read();
```

#### CsvStreamReader（Stream APIでの読み込み）

レコードをJava Streamとして扱える軽量リーダー。メモリに載せずに逐次処理したいときに最適！

```java
import com.example.csv.reader.CsvStreamReader;
import com.example.model.Person;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

// 基本（Listに集約）
List<Person> persons = CsvStreamReader.builder(Person.class, Paths.get("sample.csv"))
    .extract(stream -> stream.collect(Collectors.toList()));

// フィルタ／マップなどのStream操作
List<String> namesOver30 = CsvStreamReader.builder(Person.class, Paths.get("sample.csv"))
    .extract(stream -> stream
        .filter(p -> p.getAge() >= 30)
        .map(Person::getName)
        .collect(Collectors.toList()));
```

### CSV書き込み

#### CsvWriterWrapper（推奨）

```java
import com.example.csv.CsvWriterWrapper;
import com.example.csv.model.Person;
import java.nio.file.Paths;

List<Person> persons = Arrays.asList(
    new Person("田中太郎", 25, "エンジニア", "東京"),
    new Person("佐藤花子", 30, "デザイナー", "大阪")
);

// シンプルな書き込み
CsvWriterWrapper.builder(Person.class, Paths.get("output.csv"))
    .write(persons);

// 詳細設定
CsvWriterWrapper.builder(Person.class, Paths.get("output.tsv"))
    .charset(CharsetType.S_JIS)       // 文字セット指定
    .fileType(FileType.TSV)            // TSVファイル
    .lineSeparator(LineSeparatorType.LF) // 改行コード
    .write(persons);
```

#### CsvStreamWriter（Stream APIでの書き込み）

```java
import com.example.csv.writer.CsvStreamWriter;

CsvStreamWriter.builder(Person.class, Paths.get("output.csv"))
    .write(persons.stream());
```

### Excel読み込み

#### ExcelStreamReader

```java
import com.example.excel.reader.ExcelStreamReader;
import com.example.model.Person;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

// 基本的な読み込み
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .extract(stream -> stream.collect(Collectors.toList()));

// シート指定
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .sheetName("データ")
    .extract(stream -> stream.collect(Collectors.toList()));

// ヘッダー行の自動検出
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .headerKey("名前")
    .headerSearchRows(20)
    .extract(stream -> stream.collect(Collectors.toList()));
```

### Excel書き込み

#### ExcelStreamWriter

```java
import com.example.excel.writer.ExcelStreamWriter;

// 基本的な書き込み
ExcelStreamWriter.builder(Person.class, Paths.get("output.xlsx"))
    .sheetName("社員データ")
    .write(persons.stream());

// 既存ファイルに追記
ExcelStreamWriter.builder(Person.class, Paths.get("template.xlsx"))
    .loadExisting()           // 既存ファイルを読み込む
    .sheetName("データ")       // シート名を指定
    .startCell(2, 0)          // A3セルから書き込み開始（0ベース）
    .write(persons.stream());
```

---

## ストリーミング処理のベストプラクティス

大量データをメモリ効率よく処理するためのベストプラクティスです。

### ⚠️ 重要な注意点

**Beanに詰めて全部メモリに載せたらダメ！**

```java
// ❌ これはNG！全件メモリに載る！
List<Person> allData = ExcelStreamReader.builder(Person.class, path)
    .extract(stream -> stream.collect(Collectors.toList()));
// ↑ 10万件とかあったらメモリ不足で死ぬ！
```

### ✅ 正しい使い方のポイント

1. **forEach で1件ずつ処理** = 最もメモリ効率が良い
2. **バッチ処理** = メモリとDB性能のバランスが良い
3. **集計処理** = 超高速＆超省メモリ
4. **フィルタリング** = 必要なデータだけ処理

### 📚 詳細情報

**詳細なベストプラクティスと実装例は [ストリーミング処理のベストプラクティス](STREAMING_BEST_PRACTICES.md) を参照してください。**

---

## グルーピング処理のベストプラクティス

グルーピング処理でメモリを節約するためのベストプラクティスです。

### ⚠️ 重要な注意点

**`Collectors.groupingBy()` は全件メモリに載せる！**

```java
// ❌ これは絶対ダメ！10万件全部メモリに載る！
Map<String, List<Person>> grouped = ExcelStreamReader.builder(Person.class, path)
    .extract(stream -> stream.collect(
        Collectors.groupingBy(Person::getOccupation)
    ));
// ↑ メモリ爆発！OutOfMemoryError確定！
```

### ✅ 正しいグルーピング方法のポイント

1. **集計値のみ保持** = グルーピングで最も軽量
2. **グループごとにバッチ保存** = 大量データ向け
3. **トップNのみ保持** = ランキング処理向け
4. **フィルタリング** = 条件でデータ量を減らす

### 📚 詳細情報

**詳細なベストプラクティスと実装例は [グルーピング処理のベストプラクティス](GROUPING_BEST_PRACTICES.md) を参照してください。**

---

## 大量データ処理

### CSV外部ソート機能

**4GB～10GB程度の大きなCSVファイルをメモリに収まらなくても効率的にソートできます！**

#### 基本的な使い方

```java
import com.example.csv.CsvExternalSorter;
import java.nio.file.Paths;

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
```

#### 数値列でのソート

```java
// ID列（数値）で昇順ソート
CsvExternalSorter.builder(inputPath, outputPath)
    .comparator((line1, line2) -> {
        int id1 = Integer.parseInt(line1.split(",")[0]);
        int id2 = Integer.parseInt(line2.split(",")[0]);
        return Integer.compare(id1, id2);
    })
    .sort();
```

#### 複数列でのソート

```java
// department列 → name列の順でソート
CsvExternalSorter.builder(inputPath, outputPath)
    .comparator((line1, line2) -> {
        String[] cols1 = line1.split(",");
        String[] cols2 = line2.split(",");
        
        // まずdepartment列で比較
        int deptCompare = cols1[2].compareTo(cols2[2]);
        if (deptCompare != 0) return deptCompare;
        
        // 同じならname列で比較
        return cols1[1].compareTo(cols2[1]);
    })
    .sort();
```

#### 設定オプション

```java
CsvExternalSorter.builder(inputPath, outputPath)
    .chunkSize(500_000_000L)           // チャンクサイズ（バイト）
    .charset(CharsetType.UTF_8)        // 文字エンコーディング
    .fileType(FileType.CSV)            // ファイルタイプ
    .skipHeader(true)                  // ヘッダー行をスキップ
    .tempDirectory(Paths.get("/tmp"))  // 一時ディレクトリ
    .comparator(...)                   // ソート条件
    .sort();
```

### 大量データグルーピング＆ソート機能

**5GB以上の大容量CSV/Excelファイルをメモリ効率的にグルーピング＆ソートできる機能です。**

#### パターン1: Lambda指定 ⭐おすすめ

```java
import com.example.csv.LargeDataGroupingSorter;
import com.example.csv.model.Person;
import java.nio.file.Paths;
import java.util.Comparator;

LargeDataGroupingSorter.of(Person.class, Paths.get("huge_5gb.csv"))
    .groupBy(Person::getOccupation)                      // 職業でグルーピング
    .sortBy(Comparator.comparingInt(Person::getAge))     // 年齢でソート（昇順）
    .processGroups((groupKey, personStream) -> {
        // グループごとの処理
        System.out.println("グループ: " + groupKey);
        
        personStream.forEach(person -> {
            // 1件ずつ処理（DB保存、ファイル出力など）
            saveToDB(groupKey, person);
        });
    });
```

#### パターン2: グループごとにファイル出力

```java
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

LargeDataGroupingSorter.of(Person.class, Paths.get("huge.csv"))
    .groupBy(Person::getOccupation)
    .sortBy(Comparator.comparingInt(Person::getAge))
    .processGroups((groupKey, personStream) -> {
        // グループごとに別ファイルに出力
        Path outputFile = Paths.get("output/" + groupKey + "_sorted.csv");
        
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile)) {
            writer.write("名前,年齢,職業\n");
            
            personStream.forEach(person -> {
                try {
                    writer.write(String.format("%s,%d,%s\n",
                        person.getName(),
                        person.getAge(),
                        person.getOccupation()));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    });
```

#### 処理フロー

```
1. CSVを1行ずつ読み込み
   ↓
2. グループキーごとに一時ファイルに分割
   ↓
3. 各グループファイルを外部ソート（CsvExternalSorter使用）
   ↓
4. ソート済みグループをStreamで処理
   ↓
5. 一時ファイルを自動クリーンアップ
```

---

## DDD設計によるExcel書き込み

**DDD（ドメイン駆動設計）的な設計に基づき、`Book`、`Sheet`、`Table`のドメインモデルを使用してExcelファイルに書き込む方法です。**

複雑なExcel構造を扱う場合や、長期的なメンテナンスが必要な場合に推奨されます。

### 基本的な使い方

```java
import com.example.excel.domain.Book;
import com.example.excel.domain.Sheet;
import com.example.excel.domain.Table;
import com.example.excel.writer.BookWriter;
import com.example.model.Person;
import com.example.model.Order;
import java.nio.file.Paths;

// Personデータ
List<Person> persons = List.of(
    new Person("田中太郎", 25, "エンジニア", "東京"),
    new Person("佐藤花子", 30, "デザイナー", "大阪")
);

// Orderデータ
List<Order> orders = List.of(
    new Order("O001", "U001", 1200, "2025-01-01"),
    new Order("O002", "U002", 3000, "2025-01-02")
);

// DDDモデルを構築
Book book = Book.of(Paths.get("output.xlsx"))
    .addSheet(Sheet.of("Report")
        .addTable(Table.builder(Person.class)
            .anchor("A1")
            .data(persons)
            .build())
        .addTable(Table.builder(Order.class)
            .anchor("A20")
            .data(orders)
            .build()));

// 書き込み
BookWriter.write(book);
```

### 複数シートの書き込み

```java
Book book = Book.of(Paths.get("output.xlsx"))
    .addSheet(Sheet.of("Users")
        .addTable(Table.builder(Person.class)
            .anchor("A1")
            .data(users)
            .build()))
    .addSheet(Sheet.of("Orders")
        .addTable(Table.builder(Order.class)
            .anchor("A1")
            .data(orders)
            .build()));

BookWriter.write(book);
```

### 既存ファイルに追記

```java
Book book = Book.of(Paths.get("template.xlsx"))
    .withLoadExisting()
    .addSheet(Sheet.of("Report")
        .addTable(Table.builder(Person.class)
            .anchor("A1")
            .data(users)
            .build()));

BookWriter.write(book);
```

### Anchor値オブジェクトの使用

Anchorは文字列、Anchorオブジェクト、または行・列インデックスで指定できます。

```java
import com.example.excel.domain.Anchor;

// ① 文字列で指定
Table.builder(Person.class)
    .anchor("A1")
    .data(persons)
    .build()

// ② Anchorオブジェクトで指定
Anchor anchor = Anchor.of("B5");
Table.builder(Person.class)
    .anchor(anchor)
    .data(persons)
    .build()

// ③ 行・列インデックス（0始まり）で指定
Table.builder(Person.class)
    .anchor(4, 1)  // B5セル（0始まり: 行4=5行目、列1=B列）
    .data(persons)
    .build()
```

**注意**: 行・列のインデックスは0始まりです。
- 行0 = 1行目、行4 = 5行目
- 列0 = A列、列1 = B列

### ドメインモデルの特徴

- **`Book`**（エンティティ）: Excelファイル全体を表す
- **`Sheet`**（エンティティ）: Excelシートを表す
- **`Table`**（値オブジェクト）: Excelシート内の1つのテーブル（ブロック）を表す
- **`Anchor`**（値オブジェクト）: Excelセルの位置を表す

### 設計のメリット

1. **ドメインモデルが明確**: `Book`、`Sheet`、`Table`の関係が明確に表現される
2. **再利用性が高い**: `Sheet`や`Table`を独立してテスト・再利用可能
3. **拡張性が高い**: 将来的な機能追加に対応しやすい
4. **テストしやすい**: 各ドメインオブジェクトを個別にテスト可能
5. **不変性の保証**: `Table`や`Anchor`は値オブジェクトとして設計され、不変性を保証

---

## 移行ガイド

`CsvReaderWrapper` と `CsvWriterWrapper` が新しいBuilderパターンを導入してリファクタリングされました。
**既存のコードは完全に互換性を維持しており、すぐに動作しなくなることはありません。**

### 主な変更点

- **新しいBuilderパターン**: `builder()` メソッドを使用したより直感的なAPI
- **メソッド名の改善**: `setSkip()` → `skipLines()`, `setCharset()` → `charset()` など
- **エラーハンドリング**: `CsvReadException` / `CsvWriteException` の追加

### 📚 詳細情報

**詳細な移行方法、新旧API対応表、FAQは [移行ガイド](MIGRATION.md) を参照してください。**

---

## まとめ

### ✅ メモリに優しい方法

1. **forEach で1件ずつ処理** = 最もメモリ効率が良い
2. **バッチ処理** = メモリとDB性能のバランスが良い
3. **集計処理** = 超高速＆超省メモリ
4. **集計値のみ保持** = グルーピングで最も軽量

### ❌ やっちゃダメ

1. **collect(toList)** = 絶対ダメ！メモリ爆発！
2. **Collectors.groupingBy()** = 全件メモリに載せる！
3. **Map<String, List<Bean>>** = 全件保持でメモリ爆発！

### 🔑 キーポイント

- **全件メモリに載せない** = ストリーミング処理の本質
- **グルーピング = 全件メモリに載せる ではない！**
- **集計だけなら実データは不要**
- **バッチ処理でメモリと性能を両立**

これで大量データ処理も完璧！🔥💯✨

