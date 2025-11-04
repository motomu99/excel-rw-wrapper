# Excel RW Wrapper

OpenCSVをラップしたシンプルなCSV読み込みライブラリです。

## 特徴

- 🚀 **シンプル**: OpenCSVを簡単に使えるようにラップ
- 📦 **軽量**: 最小限の依存関係
- 🔧 **柔軟**: ファイルパス指定、InputStream対応
- 📊 **便利**: ヘッダー分離、データのみ取得などの便利機能
- 🎯 **Bean対応**: アノテーションで項目名を指定してBeanにマッピング

## 依存関係

- Java 21以上
- OpenCSV 5.9
- Lombok 1.18.30 (Beanクラスの自動生成用)

## ビルド

```bash
./gradlew build
```

## 使用方法

### CsvReaderWrapper（推奨）

**新しいBuilderパターンを使用した、最も推奨される方法です。**

#### 基本的な使い方

```java
import com.example.csv.CsvReaderWrapper;
import com.example.csv.model.Person;
import java.nio.file.Paths;
import java.util.List;

// シンプルな読み込み
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("path/to/your/file.csv"))
    .read();

// Beanのプロパティにアクセス
for (Person person : persons) {
    System.out.println("名前: " + person.getName());
    System.out.println("年齢: " + person.getAge());
}
```

#### 詳細設定

```java
import com.example.csv.CharsetType;
import com.example.csv.FileType;

// 複数の設定を組み合わせ
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("data.tsv"))
    .charset(CharsetType.S_JIS)       // 文字セット指定
    .fileType(FileType.TSV)            // TSVファイル
    .skipLines(1)                      // 最初の1行をスキップ
    .read();
```

#### 対応する文字セット

```java
CharsetType.UTF_8        // UTF-8（デフォルト）
CharsetType.UTF_8_BOM    // UTF-8 with BOM
CharsetType.S_JIS        // Shift_JIS
CharsetType.EUC_JP       // EUC-JP
CharsetType.WINDOWS_31J  // Windows-31J
```

#### ヘッダーなしCSVの読み込み

```java
// 位置ベースのマッピングを使用
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("no_header.csv"))
    .usePositionMapping()  // 位置ベースマッピング
    .read();
```

#### 従来のAPI（互換性維持）

既存コードとの互換性のため、従来の`execute()`メソッドも引き続き使用できます。

```java
List<Person> persons = CsvReaderWrapper.execute(
    Person.class,
    Paths.get("sample.csv"),
    instance -> instance.setCharset(CharsetType.UTF_8).read()
);
```

**詳細は [MIGRATION.md](MIGRATION.md) を参照してください。**

### CsvStreamReader（Stream APIでの読み込み）

レコードをJava Streamとして扱える軽量リーダー。メモリに載せずに逐次処理したいときに最適だよ！

#### 基本（Listに集約）

```java
import com.example.csv.reader.CsvStreamReader;
import com.example.model.Person;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

List<Person> persons = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .process(stream -> stream.collect(Collectors.toList()));
```

#### フィルタ／マップなどのStream操作

```java
List<String> namesOver30 = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .process(stream -> stream
        .filter(p -> p.getAge() >= 30)
        .map(Person::getName)
        .collect(Collectors.toList()));
```

#### 行スキップ

```java
List<Person> skipped = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .skip(2)
    .process(stream -> stream.collect(Collectors.toList()));
```

#### 文字セット・区切り指定（CSV/TSVなど）

```java
import com.example.common.config.CharsetType;
import com.example.common.config.FileType;

List<Person> sjis = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample_sjis.csv"))
    .charset(CharsetType.S_JIS)
    .process(stream -> stream.collect(Collectors.toList()));

List<Person> tsv = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.tsv"))
    .fileType(FileType.TSV)
    .process(stream -> stream.collect(Collectors.toList()));
```

#### ヘッダー有無のマッピング

```java
// ヘッダー付き（デフォルト）
List<Person> withHeader = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .useHeaderMapping() // 省略可（デフォルト）
    .process(stream -> stream.collect(Collectors.toList()));

// ヘッダーなし（位置ベース）
import com.example.model.PersonWithoutHeader;

List<PersonWithoutHeader> noHeader = CsvStreamReader.builder(PersonWithoutHeader.class, Paths.get("src/test/resources/sample_no_header.csv"))
    .usePositionMapping()
    .process(stream -> stream.collect(Collectors.toList()));
```

#### 戻り値なし（副作用系）

```java
CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .process(stream -> {
        stream.forEach(p -> System.out.println(p.getName()));
    });
```

#### そのほかの小技

```java
// 件数だけ欲しい場合
long count = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .process(stream -> stream.count());

// メソッドチェーンで一気に
List<String> names = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .skip(1)
    .charset(CharsetType.UTF_8)
    .fileType(FileType.CSV)
    .useHeaderMapping()
    .process(stream -> stream
        .filter(p -> p.getAge() >= 25)
        .map(Person::getName)
        .collect(Collectors.toList()));
```

> ベストプラクティスやパフォーマンスTipsは [STREAMING_BEST_PRACTICES.md](STREAMING_BEST_PRACTICES.md) もチェックしてね。

---

## CSV書き込み機能

### CsvStreamWriter（Stream APIでの書き込み）

Streamを直接書き込めるライター。`CsvStreamReader`とセットで使うと、ストリーム処理が完結するよ！

#### 基本（Streamを書き込み）

```java
import com.example.csv.writer.CsvStreamWriter;
import com.example.model.Person;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

List<Person> persons = Arrays.asList(
    new Person("田中太郎", 25, "エンジニア", "東京"),
    new Person("佐藤花子", 30, "デザイナー", "大阪")
);

CsvStreamWriter.builder(Person.class, Paths.get("output.csv"))
    .write(persons.stream());
```

#### フィルタ付き書き込み

```java
CsvStreamWriter.builder(Person.class, Paths.get("output.csv"))
    .write(persons.stream()
        .filter(p -> p.getAge() >= 30));
```

#### 文字セット・改行コード・区切り指定

```java
import com.example.common.config.CharsetType;
import com.example.common.config.FileType;
import com.example.common.config.LineSeparatorType;

CsvStreamWriter.builder(Person.class, Paths.get("output.tsv"))
    .charset(CharsetType.S_JIS)           // 文字セット指定
    .fileType(FileType.TSV)                // TSVファイル
    .lineSeparator(LineSeparatorType.LF)   // 改行コード
    .write(persons.stream());
```

#### ヘッダー有無のマッピング

```java
// ヘッダー付き（デフォルト）
CsvStreamWriter.builder(Person.class, Paths.get("output.csv"))
    .useHeaderMapping() // 省略可（デフォルト）
    .write(persons.stream());

// ヘッダーなし（位置ベース）
import com.example.model.PersonWithoutHeader;

CsvStreamWriter.builder(PersonWithoutHeader.class, Paths.get("output.csv"))
    .usePositionMapping()
    .write(persons.stream());
```

#### メソッドチェーンで一気に

```java
CsvStreamWriter.builder(Person.class, Paths.get("output.csv"))
    .charset(CharsetType.UTF_8)
    .fileType(FileType.CSV)
    .lineSeparator(LineSeparatorType.LF)
    .useHeaderMapping()
    .write(persons.stream()
        .filter(p -> p.getAge() >= 25)
        .filter(p -> !p.getOccupation().equals("学生")));
```

#### CsvStreamReaderと組み合わせて使う

```java
// 読み込み → フィルタ → 書き込みの一連の流れ
CsvStreamReader.builder(Person.class, Paths.get("input.csv"))
    .process(stream -> {
        CsvStreamWriter.builder(Person.class, Paths.get("output.csv"))
            .write(stream.filter(p -> p.getAge() >= 30));
    });
```

---

### CsvWriterWrapper（推奨）

**新しいBuilderパターンを使用した、最も推奨される方法です。**

#### 基本的な使い方

```java
import com.example.csv.CsvWriterWrapper;
import com.example.csv.model.Person;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

// シンプルな書き込み
List<Person> persons = Arrays.asList(
    new Person("田中太郎", 25, "エンジニア", "東京"),
    new Person("佐藤花子", 30, "デザイナー", "大阪")
);

CsvWriterWrapper.builder(Person.class, Paths.get("output.csv"))
    .write(persons);
```

#### 詳細設定

```java
import com.example.csv.CharsetType;
import com.example.csv.FileType;
import com.example.csv.LineSeparatorType;

// 複数の設定を組み合わせ
CsvWriterWrapper.builder(Person.class, Paths.get("output.tsv"))
    .charset(CharsetType.S_JIS)       // 文字セット指定
    .fileType(FileType.TSV)            // TSVファイル
    .lineSeparator(LineSeparatorType.LF) // 改行コード
    .write(persons);
```

#### 対応する改行コード

```java
LineSeparatorType.CRLF   // Windows標準（\r\n）（デフォルト）
LineSeparatorType.LF     // Unix/Linux/Mac標準（\n）
LineSeparatorType.CR     // 旧Mac標準（\r）
```

#### ヘッダーなしCSVの書き込み

```java
// 位置ベースのマッピングを使用
CsvWriterWrapper.builder(Person.class, Paths.get("no_header.csv"))
    .usePositionMapping()  // 位置ベースマッピング
    .write(persons);
```

#### 従来のAPI（互換性維持）

既存コードとの互換性のため、従来の`execute()`メソッドも引き続き使用できます。

```java
CsvWriterWrapper.execute(
    Person.class,
    Paths.get("output.csv"),
    instance -> instance.setCharset(CharsetType.UTF_8).write(persons)
);
```

**詳細は [MIGRATION.md](MIGRATION.md) を参照してください。**

---

## 大容量CSV外部ソート機能 🚀

**4GB～10GB程度の大きなCSVファイルをメモリに収まらなくても効率的にソートできます！**

### 特徴

- ✨ **メモリ効率**: 大きなファイルをチャンクに分割して処理
- 🎯 **柔軟なソート**: 任意の列や複数列でのソートに対応
- ⚡ **高速処理**: k-wayマージソートアルゴリズムを使用
- 🧹 **自動クリーンアップ**: 一時ファイルを自動的に削除

### 基本的な使い方

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

### 数値列でのソート

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

### 複数列でのソート

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

### 設定オプション

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

**詳細は [EXTERNAL_SORT_USAGE.md](EXTERNAL_SORT_USAGE.md) を参照してください。**

---

## アノテーションでの項目名指定

```java
import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    @CsvBindByName(column = "名前")
    private String name;
    
    @CsvBindByName(column = "年齢")
    private Integer age;
    
    @CsvBindByName(column = "職業")
    private String occupation;
    
    @CsvBindByName(column = "出身地")
    private String birthplace;
    
    // Lombokがgetter/setter/toString/equals/hashCodeを自動生成
}
```

### その他のOpenCSVアノテーション

```java
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvDate;
import com.opencsv.bean.CsvNumber;

public class Employee {
    // ヘッダー名でマッピング
    @CsvBindByName(column = "id")
    private Long id;
    
    // 位置でマッピング
    @CsvBindByPosition(position = 1)
    private String name;
    
    // 日付フォーマット指定
    @CsvBindByName(column = "hire_date")
    @CsvDate("yyyy-MM-dd")
    private LocalDate hireDate;
    
    // 数値フォーマット指定
    @CsvBindByName(column = "salary")
    @CsvNumber("#,##0")
    private Integer salary;
}
```

### 日付フィールドの指定

```java
public class Employee {
    @CsvBindByName(column = "hire_date")
    @CsvDate("yyyy-MM-dd")
    private LocalDate hireDate;

    // Getter/Setter
    // ...
}
```

## サンプルファイル

テスト用のサンプルCSVファイルが含まれています：

- `src/test/resources/sample.csv` - 日本語サンプルデータ
- `src/test/resources/employees.csv` - 英語サンプルデータ

## テスト実行

```bash
./gradlew test
```

## テストカバレッジ

```bash
./gradlew test jacocoTestReport
```

カバレッジレポートは `build/reports/jacoco/test/html/index.html` で確認できます。

## Javadoc

APIドキュメントを生成できます：

### Javadocの生成
```bash
# HTMLドキュメントの生成
./gradlew generateJavadoc

# JavadocJARの生成
./gradlew javadocJar
```

### 生成されるファイル
- **HTMLドキュメント**: `build/docs/javadoc/index.html`
- **JavadocJAR**: `build/libs/excel-rw-wrapper-1.0.0-javadoc.jar`

### ビルド時の自動生成
通常のビルド時にもJavadocが自動生成されます：
```bash
./gradlew build
```

## ライセンス

MIT License
