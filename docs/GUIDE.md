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
    .process(stream -> stream.collect(Collectors.toList()));

// フィルタ／マップなどのStream操作
List<String> namesOver30 = CsvStreamReader.builder(Person.class, Paths.get("sample.csv"))
    .process(stream -> stream
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
    .process(stream -> stream.collect(Collectors.toList()));

// シート指定
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .sheetName("データ")
    .process(stream -> stream.collect(Collectors.toList()));

// ヘッダー行の自動検出
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .headerKey("名前")
    .headerSearchRows(20)
    .process(stream -> stream.collect(Collectors.toList()));
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

### ⚠️ 重要な注意点

**Beanに詰めて全部メモリに載せたらダメ！**

```java
// ❌ これはNG！全件メモリに載る！
List<Person> allData = ExcelStreamReader.builder(Person.class, path)
    .process(stream -> stream.collect(Collectors.toList()));
// ↑ 10万件とかあったらメモリ不足で死ぬ！
```

### ✅ 正しい使い方

#### 1. forEach で1件ずつDB保存（メモリ最小）

```java
ExcelStreamReader.builder(Person.class, path)
    .process(stream -> {
        stream.forEach(person -> {
            // 1件ずつDB保存
            personRepository.save(person);
        });
        return null;
    });

// 💡 メモリ使用量: 常に100行分程度（数MB）
// 💡 100万行でも問題なし！
```

#### 2. バッチ処理（100件ごとにまとめて保存）

```java
List<Person> batch = new ArrayList<>();
final int BATCH_SIZE = 100;

ExcelStreamReader.builder(Person.class, path)
    .process(stream -> {
        stream.forEach(person -> {
            batch.add(person);
            
            // 100件たまったらまとめて保存
            if (batch.size() >= BATCH_SIZE) {
                personRepository.saveAll(batch);  // バッチ保存
                batch.clear();  // ⭐ メモリ解放！
            }
        });
        
        // 残りを保存
        if (!batch.isEmpty()) {
            personRepository.saveAll(batch);
            batch.clear();
        }
        return null;
    });

// 💡 メモリ使用量: バッチサイズ分（100件=数MB）
// 💡 DB保存の効率も良い！
```

#### 3. 集計処理（全件メモリに載せずに集計）

```java
// 件数カウント
long totalCount = ExcelStreamReader.builder(Person.class, path)
    .process(stream -> stream.count());

// 平均年齢
double averageAge = ExcelStreamReader.builder(Person.class, path)
    .process(stream -> stream
        .mapToInt(Person::getAge)
        .average()
        .orElse(0.0));

// 💡 メモリ使用量: 集計値のみ（数バイト）
// 💡 100万行でも一瞬で集計可能！
```

#### 4. フィルタリング＋1件ずつ処理

```java
ExcelStreamReader.builder(Person.class, path)
    .process(stream -> {
        stream
            .filter(person -> person.getAge() >= 30)  // 30歳以上
            .filter(person -> "東京".equals(person.getBirthplace()))  // 東京在住
            .forEach(person -> {
                // 条件に合った人だけ処理
                sendEmail(person);
            });
        return null;
    });

// 💡 フィルタリングされた分だけ処理されるから超高速！
```

#### 5. 必要な件数だけ処理（早期終了）

```java
ExcelStreamReader.builder(Person.class, path)
    .process(stream -> {
        stream
            .limit(1000)  // 最初の1000件だけ
            .forEach(person -> {
                // 処理
            });
        return null;
    });

// 💡 残りのデータは読み込まない！超高速！
```

### ❌ やっちゃダメなこと

1. **全件をListに格納**
```java
// ❌ NG！
List<Person> all = ExcelStreamReader.builder(Person.class, path)
    .process(stream -> stream.collect(Collectors.toList()));
```

2. **全件をMapに格納**
```java
// ❌ NG！
Map<String, Person> map = ExcelStreamReader.builder(Person.class, path)
    .process(stream -> stream.collect(
        Collectors.toMap(Person::getName, p -> p)
    ));
```

3. **中間でListを作成**
```java
// ❌ NG！
ExcelStreamReader.builder(Person.class, path)
    .process(stream -> {
        List<Person> list = stream.collect(Collectors.toList());  // 全件メモリに！
        list.forEach(p -> save(p));  // これじゃ意味ない
        return null;
    });
```

### 💡 ポイント

| 項目 | 従来の方法 | ストリーミング処理 |
|------|-----------|------------------|
| **メモリ使用量** | 全件分（数GB） | 常に100行分（数MB） |
| **処理速度** | 全件読み込み後に処理 | 読み込みながら処理 |
| **大量データ対応** | ❌ OutOfMemoryError | ✅ 100万行でもOK |
| **推奨される処理** | 小規模データのみ | 全ての場合 |

### 🚀 実装のコツ

#### Spring Bootでの実装例

```java
@Service
public class PersonImportService {
    
    @Autowired
    private PersonRepository personRepository;
    
    @Transactional
    public void importFromExcel(Path excelPath) throws IOException {
        List<Person> batch = new ArrayList<>();
        final int BATCH_SIZE = 1000;
        
        ExcelStreamReader.builder(Person.class, excelPath)
            .headerKey("名前")  // ヘッダー自動検出
            .process(stream -> {
                stream.forEach(person -> {
                    batch.add(person);
                    
                    if (batch.size() >= BATCH_SIZE) {
                        personRepository.saveAll(batch);
                        personRepository.flush();  // メモリ解放
                        batch.clear();
                    }
                });
                
                // 残りを保存
                if (!batch.isEmpty()) {
                    personRepository.saveAll(batch);
                }
                
                return null;
            });
    }
}
```

---

## グルーピング処理のベストプラクティス

### ⚠️ 重要な注意点

**`Collectors.groupingBy()` は全件メモリに載せる！**

```java
// ❌ これは絶対ダメ！10万件全部メモリに載る！
Map<String, List<Person>> grouped = ExcelStreamReader.builder(Person.class, path)
    .process(stream -> stream.collect(
        Collectors.groupingBy(Person::getOccupation)
    ));
// ↑ メモリ爆発！OutOfMemoryError確定！
```

### ✅ 正しいグルーピング方法

#### 1. 集計値のみ保持（メモリ最小）⭐おすすめ！

**グループごとの件数・平均・合計だけ保持**

```java
// 職業ごとの統計情報を保持（実データは保持しない）
Map<String, OccupationStats> statsMap = new ConcurrentHashMap<>();

ExcelStreamReader.builder(Person.class, path)
    .process(stream -> {
        stream.forEach(person -> {
            String occupation = person.getOccupation();
            statsMap.computeIfAbsent(occupation, k -> new OccupationStats())
                   .add(person.getAge());
        });
        return null;
    });

// 結果
statsMap.forEach((occupation, stats) -> {
    System.out.println(occupation + ":");
    System.out.println("  人数: " + stats.getCount());
    System.out.println("  平均年齢: " + stats.getAverage());
});

// 💡 メモリ: グループ数×集計値のみ（数KB）
// 💡 10万件のデータは保持していない！

// OccupationStatsクラス
class OccupationStats {
    private int count = 0;
    private long sum = 0;
    
    public void add(int age) {
        count++;
        sum += age;
    }
    
    public int getCount() { return count; }
    public double getAverage() { return count == 0 ? 0 : (double) sum / count; }
}
```

#### 2. グループごとにバッチ保存（大量データ向け）⭐おすすめ！

**各グループごとに100件ずつDB保存**

```java
final int BATCH_SIZE = 100;
Map<String, List<Person>> batchMap = new HashMap<>();

ExcelStreamReader.builder(Person.class, path)
    .process(stream -> {
        stream.forEach(person -> {
            String occupation = person.getOccupation();
            
            // 職業ごとのバッチに追加
            batchMap.computeIfAbsent(occupation, k -> new ArrayList<>())
                   .add(person);
            
            // バッチサイズに達したらDB保存
            List<Person> batch = batchMap.get(occupation);
            if (batch.size() >= BATCH_SIZE) {
                personRepository.saveAllByOccupation(occupation, batch);
                batch.clear();  // ⭐ メモリ解放！
            }
        });
        
        // 残りを保存
        batchMap.forEach((occupation, batch) -> {
            if (!batch.isEmpty()) {
                personRepository.saveAllByOccupation(occupation, batch);
                batch.clear();
            }
        });
        
        return null;
    });

// 💡 メモリ: グループ数×バッチサイズ
// 💡 例：5グループ×100件 = 500件分だけメモリに！
```

#### 3. トップNのみ保持（ランキング処理）

**各グループの上位10件だけ保持**

```java
Map<String, TopNCollector> topNMap = new HashMap<>();
final int TOP_N = 10;

ExcelStreamReader.builder(Person.class, path)
    .process(stream -> {
        stream.forEach(person -> {
            String city = person.getBirthplace();
            topNMap.computeIfAbsent(city, k -> new TopNCollector(TOP_N))
                  .add(person);
        });
        return null;
    });

// 結果
topNMap.forEach((city, topN) -> {
    System.out.println(city + "（年齢トップ10）:");
    topN.getTop().forEach(person -> 
        System.out.println("  - " + person.getName() + " (" + person.getAge() + "歳)")
    );
});

// 💡 メモリ: グループ数×トップN件
// 💡 例：5グループ×10件 = 50件分だけ！

// TopNCollectorクラス
class TopNCollector {
    private final int maxSize;
    private final PriorityQueue<Person> queue;
    
    public TopNCollector(int maxSize) {
        this.maxSize = maxSize;
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
        result.sort(Comparator.comparingInt(Person::getAge).reversed());
        return result;
    }
}
```

### 🎯 使い分けガイド

| ケース | 推奨方法 | メモリ使用量 |
|--------|---------|-------------|
| **集計だけ必要** | 方法1: 集計値のみ | 数KB |
| **グループごとにDB保存** | 方法2: バッチ保存 | 数MB |
| **ランキング表示** | 方法3: トップN保持 | 数十KB |
| **条件付き集計** | 方法4: フィルタ＋集計 | 数バイト |
| **グループ数が超少ない** | 方法5: 制限付き保持 | 要注意 |

### 💡 判断フローチャート

```
グルーピング処理が必要
    ↓
実データが必要？
    ├─ NO → 集計値のみ保持（方法1）⭐最軽量
    └─ YES
         ↓
    グループ数は？
         ├─ 少ない（10個以下）
         │    ↓
         │  各グループのデータ量は？
         │    ├─ 少ない（1000件以下/グループ）→ 制限付き保持もOK
         │    └─ 多い → バッチ処理（方法2）
         └─ 多い（10個以上）
              ↓
         全件必要？
              ├─ NO → トップNのみ保持（方法3）
              └─ YES → バッチ処理（方法2）⭐推奨
```

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

### 概要

`CsvReaderWrapper` と `CsvWriterWrapper` が新しいBuilderパターンを導入してリファクタリングされました。
**既存のコードは完全に互換性を維持しており、すぐに動作しなくなることはありません。**

しかし、新しいBuilderパターンはより直感的で読みやすいため、今後の開発では新しいAPIの使用を推奨します。

### 変更内容

#### CsvReaderWrapper

**Before (従来のAPI - 引き続き使用可能)**
```java
List<Person> persons = CsvReaderWrapper.execute(
    Person.class,
    Paths.get("sample.csv"),
    instance -> instance.setCharset(CharsetType.UTF_8_BOM).setSkip(1).read()
);
```

**After (新しいAPI - 推奨)**
```java
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("sample.csv"))
    .charset(CharsetType.UTF_8_BOM)
    .skipLines(1)
    .read();
```

#### CsvWriterWrapper

**Before (従来のAPI - 引き続き使用可能)**
```java
List<Person> persons = Arrays.asList(new Person("田中", 25));
CsvWriterWrapper.execute(
    Person.class,
    Paths.get("output.csv"),
    instance -> instance.setCharset(CharsetType.UTF_8).write(persons)
);
```

**After (新しいAPI - 推奨)**
```java
List<Person> persons = Arrays.asList(new Person("田中", 25));
CsvWriterWrapper.builder(Person.class, Paths.get("output.csv"))
    .charset(CharsetType.UTF_8)
    .write(persons);
```

### 移行方法

#### ステップ1: 既存コードの動作確認

まず、現在のコードがそのまま動作することを確認してください。

```bash
# テストを実行
./gradlew test
```

#### ステップ2: 新しいAPIへの段階的移行

**重要: 一度にすべてを変更する必要はありません。**
プロジェクトの開発サイクルに合わせて、段階的に移行してください。

#### 推奨移行順序

1. **新機能・新規実装**: 新しいBuilderパターンを使用
2. **既存コードの修正時**: 該当部分を新しいAPIに移行
3. **リファクタリング期間**: 既存コード全体を徐々に移行

### 新旧API対応表

| 従来のAPI | 新しいAPI | 説明 |
|-----------|-----------|------|
| `execute()` | `builder()` | エントリーポイント |
| `setSkip(n)` | `skipLines(n)` | より明確な命名 |
| `setCharset(type)` | `charset(type)` | より簡潔な命名 |
| `setFileType(type)` | `fileType(type)` | より簡潔な命名 |
| `usePositionMapping()` | `usePositionMapping()` | 変更なし |
| `useHeaderMapping()` | `useHeaderMapping()` | 変更なし |

### FAQ

#### Q1: 既存のコードはすぐに変更する必要がありますか？

**A:** いいえ、必要ありません。従来の `execute()` メソッドは完全に互換性を維持しており、引き続き使用できます。ただし、新しいコードでは `builder()` メソッドの使用を推奨します。

#### Q2: いつまでに移行する必要がありますか？

**A:** 明確な期限はありません。プロジェクトの開発サイクルに合わせて、段階的に移行することを推奨します。

#### Q3: 新旧APIを混在させても問題ありませんか？

**A:** はい、問題ありません。同一プロジェクト内で両方のAPIを使用できます。

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

