# Excel RW Wrapper

OpenCSVをラップしたシンプルなCSV読み込みライブラリです。

## 特徴

- 🚀 **シンプル**: OpenCSVを簡単に使えるようにラップ
- 📦 **軽量**: 最小限の依存関係
- 🔧 **柔軟**: ファイルパス指定、InputStream対応
- 📊 **便利**: ヘッダー分離、データのみ取得などの便利機能
- 🎯 **Bean対応**: アノテーションで項目名を指定してBeanにマッピング
- ✅ **列数検証**: CSV/TSVファイルの列数不整合を自動検出し、エラーを早期に検知
- 🔢 **行番号トラッキング**: データの元ファイル行番号を自動取得してエラー特定を容易に
- 📝 **フリガナ対応**: Excelセルの末尾フリガナを自動的に削除して読み込み（FastExcel Reader使用）

## 依存関係

- Java 21以上
- OpenCSV 5.9 (CSV読み書き用)
- Apache POI 5.2.5 (Excel読み書き用)
- FastExcel Reader 0.19.0 (大容量Excel読み込み用)
- Lombok 1.18.30 (Beanクラスの自動生成用)

## 📚 ドキュメント

**完全ガイド**: [docs/GUIDE.md](docs/GUIDE.md) - 全機能とベストプラクティスをまとめた統合ガイド

**機能一覧**: [docs/FEATURE_LIST.md](docs/FEATURE_LIST.md) - 全機能の一覧と各機能へのリンク

### 個別ガイド

- [ストリーミング処理のベストプラクティス](docs/STREAMING_BEST_PRACTICES.md) - 大量データをメモリ効率よく処理する方法
- [グルーピング処理のベストプラクティス](docs/GROUPING_BEST_PRACTICES.md) - グルーピング処理でメモリを節約する方法
- [大量データグルーピング＆ソート機能](docs/LARGE_DATA_GROUPING_USAGE.md) - 5GB以上の大容量ファイルを処理
- [CSV外部ソート機能](docs/EXTERNAL_SORT_USAGE.md) - 4GB～10GBのCSVファイルをソート
- [DDD設計によるExcel書き込み](docs/DDD_DESIGN_EXAMPLE.md) - DDD設計パターンでのExcel書き込み
- [移行ガイド](docs/MIGRATION.md) - 新しいBuilderパターンへの移行方法

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
    .fileType(FileType.TSV)           // TSVファイル
    .skipLines(1)                     // 最初の1行をスキップ
    .read();

// ダブルクォートの扱いを緩くしたいTSV/CSV（エスケープされていない\"が混ざる等）の場合
List<Person> looseQuoted = CsvReaderWrapper.builder(Person.class, Paths.get("data_with_quotes.tsv"))
    .fileType(FileType.TSV)
    .ignoreQuotations(true)           // クォートを通常文字として扱う（列数チェックにも適用）
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

#### 行番号トラッキング機能 🔢

ファイル読み込み時に、データの元ファイル行番号を自動的に取得できます。エラー発生時の行特定やデータ検証に便利です。

**実装方法は4つ（お好みで選択）:**

##### 方法1: 抽象クラス継承（最も簡単 ⭐推奨）

```java
import com.example.common.model.LineNumberAware;

@Data
@EqualsAndHashCode(callSuper = true)
public class Person extends LineNumberAware {
    @CsvBindByName(column = "名前")
    private String name;

    @CsvBindByName(column = "年齢")
    private Integer age;
}

// 使用例
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("data.csv"))
    .read();

persons.forEach(person -> {
    System.out.println("行番号: " + person.getLineNumber()); // 行番号が自動設定される
    System.out.println("名前: " + person.getName());
});
```

##### 方法2: インターフェース実装（既に他のクラスを継承している場合）

```java
import com.example.common.model.ILineNumberAware;
import com.example.common.annotation.LineNumber;

@Data
public class Person implements ILineNumberAware {
    @LineNumber  // このアノテーションが必要
    private Integer lineNumber;

    @CsvBindByName(column = "名前")
    private String name;
}
```

##### 方法3: アノテーションのみ（柔軟に使いたい場合）

```java
import com.example.common.annotation.LineNumber;

@Data
public class Person {
    @LineNumber  // このフィールドに行番号が自動設定される
    private Integer lineNumber;

    @CsvBindByName(column = "名前")
    private String name;
}
```

##### 方法4: RowDataラッパー（既存モデルを変更したくない場合）

既存のドメインモデルを変更せずに行番号情報を取得したい場合に使用します。

```java
import com.example.common.model.RowData;

// 既存のモデルクラス（変更不要）
@Data
public class Person {
    @CsvBindByName(column = "名前")
    private String name;

    @CsvBindByName(column = "年齢")
    private Integer age;
}

// 使用方法: readWithLineNumber() メソッドを使用
List<RowData<Person>> results = CsvReaderWrapper.builder(Person.class, Paths.get("data.csv"))
    .readWithLineNumber();

results.forEach(row -> {
    System.out.println("行番号: " + row.getLineNumber());
    System.out.println("名前: " + row.getData().getName());
});

// ストリーム処理での使用例
results.stream()
    .filter(row -> row.getLineNumber() > 10)
    .map(RowData::getData)
    .forEach(person -> System.out.println(person.getName()));
```

**メリット:**
- ドメインモデルを変更する必要がない
- 行番号が必要な場合のみ使用できる
- 既存コードへの影響が最小限

**デメリット:**
- データ取得時に `getData()` の呼び出しが必要
- ラッピング/アンラッピングの手間がある

**動作:**
- **ヘッダーあり**（`@CsvBindByName`使用時）: 行番号は **2** から開始（1行目はヘッダー）
- **ヘッダーなし**（`@CsvBindByPosition`使用時）: 行番号は **1** から開始
- Excel/CSVの両方で動作
- ストリーミング処理（`CsvStreamReader`、`ExcelStreamReader`）でも使用可能
- `@CsvCustomBindByName`、`@CsvCustomBindByPosition`にも対応

#### 列数検証機能

CSV/TSVファイルを読み込む前に、自動的に列数の整合性をチェックします。ダブルクォートが外れている行や、タブ/カンマの数が他の行と異なる行を検出し、`CsvReadException`をスローします。

```java
// 列数が不一致のファイルを読み込もうとすると例外が発生
try {
    List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("invalid.csv"))
        .read();
} catch (CsvReadException e) {
    // エラーメッセージには行番号、期待される列数、実際の列数、行内容が含まれます
    // 例: "列数が不一致です (ファイル=invalid.csv, 行番号=3, 期待値=4, 実際=5, 行内容=...)")
    System.err.println(e.getMessage());
}
```

**検証の仕組み:**
- 最初の非空行（通常はヘッダー行）の列数を基準として設定
- 以降の各行の列数をチェック
- 列数が不一致の場合、即座に`CsvReadException`をスロー
- CSV/TSVの両方で動作（区切り文字は自動判定）
- ダブルクォート内の区切り文字は正しく処理されます

#### 列数不一致でも最後まで読み込む機能（readWithValidation）

列数が不一致の行があっても処理を止めず、最後まで読み込んでエラー行の情報を取得できます。

**CSVの場合:**
```java
// 列数不一致の行をスキップして最後まで読み込む
CsvReadResult<Person> result = CsvReaderWrapper.builder(Person.class, Paths.get("sample.csv"))
    .readWithValidation();

// 成功した行のデータを取得
List<Person> data = result.getData();

// エラー行の情報を取得
List<CsvReadError> errors = result.getErrors();

// エラーがあるかチェック
if (result.hasErrors()) {
    System.out.println("エラー行数: " + result.getErrorCount());
    errors.forEach(error -> {
        System.out.println("行番号: " + error.getLineNumber() + 
                          ", 期待値: " + error.getExpectedColumnCount() +
                          ", 実際: " + error.getActualColumnCount());
    });
}
```

**Excelの場合:**
```java
// 列数不一致の行をスキップして最後まで読み込む
ExcelReadResult<Person> result = ExcelReader.builder(Person.class, Paths.get("sample.xlsx"))
    .readWithValidation();

// 成功した行のデータを取得
List<Person> data = result.getData();

// エラー行の情報を取得
List<ExcelReadError> errors = result.getErrors();

// エラーがあるかチェック
if (result.hasErrors()) {
    System.out.println("エラー行数: " + result.getErrorCount());
    errors.forEach(error -> {
        System.out.println("行番号: " + error.getLineNumber() + 
                          ", 期待値: " + error.getExpectedColumnCount() +
                          ", 実際: " + error.getActualColumnCount());
    });
}
```

**動作:**
- 列数不一致の行はスキップされ、エラー情報として記録されます
- 処理は最後まで続行されます
- 成功した行のデータとエラー行の情報の両方が返されます
- CSV/TSV/Excelの全てで動作します
- `CsvStreamReader`と`ExcelStreamReader`でも使用可能です

**注意:** `readWithValidation()`は単一ファイルのみサポートしています。複数ファイルの場合は通常の`read()`メソッドを使用してください。

#### 従来のAPI（互換性維持）

既存コードとの互換性のため、従来の`execute()`メソッドも引き続き使用できます。

```java
List<Person> persons = CsvReaderWrapper.execute(
    Person.class,
    Paths.get("sample.csv"),
    instance -> instance.setCharset(CharsetType.UTF_8).read()
);
```

**詳細は [docs/MIGRATION.md](docs/MIGRATION.md) を参照してください。**

#### 複数ファイルの並列読み込み 🔄

複数のCSVファイルを並列に読み込み、順序を維持して結合することができます。

```java
import java.util.Arrays;

List<Path> files = Arrays.asList(
    Paths.get("data1.csv"),
    Paths.get("data2.csv")
);

// 複数ファイルを並列処理で一括読み込み
List<Person> persons = CsvReaderWrapper.builder(Person.class, files)
    .skipLines(1)                // 全ファイル共通の設定
    .parallelism(4)              // 4並列で読み込み（順序は維持されます！）
    .readAll();                  // 全ファイルを結合してListで返す
```

### CsvStreamReader（Stream APIでの読み込み）

レコードをJava Streamとして扱える軽量リーダー。メモリに載せずに逐次処理したいときに最適だよ！

**注意:** `CsvStreamReader`も`CsvReaderWrapper`と同様に、読み込み前に自動的に列数検証が行われます。列数が不一致の場合は`CsvReadException`がスローされます。

**列数不一致でも最後まで読み込む機能:**
`CsvStreamReader`でも`readWithValidation()`メソッドを使用することで、列数不一致の行をスキップして最後まで読み込むことができます。

```java
// CsvStreamReaderで列数不一致の行をスキップして最後まで読み込む
CsvReadResult<Person> result = CsvStreamReader.builder(Person.class, Paths.get("sample.csv"))
    .readWithValidation();

// 成功した行のデータとエラー行の情報を取得
List<Person> data = result.getData();
List<CsvReadError> errors = result.getErrors();
```

#### 基本（Listに集約）

```java
import com.example.csv.reader.CsvStreamReader;
import com.example.model.Person;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

List<Person> persons = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .extract(stream -> stream.collect(Collectors.toList()));
```

#### フィルタ／マップなどのStream操作

```java
List<String> namesOver30 = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .extract(stream -> stream
        .filter(p -> p.getAge() >= 30)
        .map(Person::getName)
        .collect(Collectors.toList()));
```

#### 行スキップ

```java
List<Person> skipped = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .skip(2)
    .extract(stream -> stream.collect(Collectors.toList()));
```

#### 文字セット・区切り指定（CSV/TSVなど）

```java
import com.example.common.config.CharsetType;
import com.example.common.config.FileType;

List<Person> sjis = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample_sjis.csv"))
    .charset(CharsetType.S_JIS)
    .extract(stream -> stream.collect(Collectors.toList()));

List<Person> tsv = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.tsv"))
    .fileType(FileType.TSV)
    .extract(stream -> stream.collect(Collectors.toList()));

// クォート無視オプション（ダブルクォートの崩れがあるTSVなど向け）
List<Person> tsvLoose = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.tsv"))
    .fileType(FileType.TSV)
    .ignoreQuotations(true)           // 列数検証・一時ファイル作成時にクォートを通常文字として扱う
    .extract(stream -> stream.collect(Collectors.toList()));
```

#### ヘッダー有無のマッピング

```java
// ヘッダー付き（デフォルト）
List<Person> withHeader = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .useHeaderMapping() // 省略可（デフォルト）
    .extract(stream -> stream.collect(Collectors.toList()));

// ヘッダーなし（位置ベース）
import com.example.model.PersonWithoutHeader;

List<PersonWithoutHeader> noHeader = CsvStreamReader.builder(PersonWithoutHeader.class, Paths.get("src/test/resources/sample_no_header.csv"))
    .usePositionMapping()
    .extract(stream -> stream.collect(Collectors.toList()));
```

#### 戻り値なし（副作用系）

```java
CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .consume(stream -> {
        stream.forEach(p -> System.out.println(p.getName()));
    });
```

#### そのほかの小技

```java
// 件数だけ欲しい場合
long count = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .extract(stream -> stream.count());

// メソッドチェーンで一気に
List<String> names = CsvStreamReader.builder(Person.class, Paths.get("src/test/resources/sample.csv"))
    .skip(1)
    .charset(CharsetType.UTF_8)
    .fileType(FileType.CSV)
    .useHeaderMapping()
    .extract(stream -> stream
        .filter(p -> p.getAge() >= 25)
        .map(Person::getName)
        .collect(Collectors.toList()));
```

> ベストプラクティスやパフォーマンスTipsは [docs/STREAMING_BEST_PRACTICES.md](docs/STREAMING_BEST_PRACTICES.md) もチェックしてね。

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

#### ヘッダーなし・クオート制御

```java
import com.example.common.config.QuoteStrategy;

// ヘッダーを出力しない
CsvStreamWriter.builder(Person.class, Paths.get("no_header.csv"))
    .noHeader()
    .write(persons.stream());

// クオートを最小限にする（区切り文字や改行を含む場合のみクオート）
CsvStreamWriter.builder(Person.class, Paths.get("minimal_quote.csv"))
    .quoteStrategy(QuoteStrategy.MINIMAL)
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
    .consume(stream -> {
        CsvStreamWriter.builder(Person.class, Paths.get("output.csv"))
            .write(stream.filter(p -> p.getAge() >= 30));
    });
```

#### ExcelStreamWriterと組み合わせて使う（CSV → Excel変換）

```java
import com.example.excel.writer.ExcelStreamWriter;

// CSVから読み込んでExcelに書き込む
CsvStreamReader.builder(Person.class, Paths.get("input.csv"))
    .consume(stream -> {
        ExcelStreamWriter.builder(Person.class, Paths.get("output.xlsx"))
            .sheetName("社員データ")
            .write(stream);
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

#### ヘッダー出力制御とクオート戦略

```java
import com.example.common.config.QuoteStrategy;

// ヘッダーを出力しない
CsvWriterWrapper.builder(Person.class, Paths.get("no_header.csv"))
    .noHeader()
    .write(persons);

// クオートを必要な時だけに
CsvWriterWrapper.builder(Person.class, Paths.get("minimal_quote.csv"))
    .quoteStrategy(QuoteStrategy.MINIMAL)
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

**詳細は [docs/MIGRATION.md](docs/MIGRATION.md) を参照してください。**

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

**詳細は [docs/EXTERNAL_SORT_USAGE.md](docs/EXTERNAL_SORT_USAGE.md) を参照してください。**

---

## Excel読み込み機能 📊

Apache POIとFastExcel ReaderをラップしたシンプルなExcel読み込みライブラリです。フリガナ付きセルの末尾フリガナも正しく処理できます。ヘッダー名は前後空白をトリムした上で厳密一致です。

### ExcelStreamReader（Stream APIでの読み込み）

レコードをJava Streamとして扱えるExcelリーダー。メモリ効率の良いストリーミング処理が可能！

#### 基本的な使い方

```java
import com.example.excel.reader.ExcelStreamReader;
import com.example.model.Person;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

// 基本的な読み込み（Listに集約）
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .extract(stream -> stream.collect(Collectors.toList()));
```

#### シート指定

```java
// シートインデックスで指定（0から始まる）
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .sheetIndex(0)
    .extract(stream -> stream.collect(Collectors.toList()));

// シート名で指定
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .sheetName("データ")
    .extract(stream -> stream.collect(Collectors.toList()));
```

#### ヘッダー行の自動検出

```java
// ヘッダー行を自動検出（上から10行以内で「名前」列を探す）
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .headerKey("名前")
    .extract(stream -> stream.collect(Collectors.toList()));

// ヘッダー行の探索範囲を20行に拡張
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .headerKey("名前")
    .headerSearchRows(20)
    .extract(stream -> stream.collect(Collectors.toList()));
```

#### 行のスキップ

```java
// 最初の2行をスキップ（タイトル行などがある場合）
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .skip(2)
    .extract(stream -> stream.collect(Collectors.toList()));
```

#### ヘッダーなしExcelの読み込み

```java
// 位置ベースのマッピングを使用
List<PersonWithoutHeader> persons = ExcelStreamReader.builder(PersonWithoutHeader.class, Paths.get("no_header.xlsx"))
    .usePositionMapping()
    .extract(stream -> stream.collect(Collectors.toList()));
```

#### フィルタ／マップなどのStream操作

```java
// 年齢30歳以上でフィルタ
List<Person> filtered = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .extract(stream -> stream
        .filter(p -> p.getAge() >= 30)
        .collect(Collectors.toList()));

// 名前だけを抽出
List<String> names = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .extract(stream -> stream
        .map(Person::getName)
        .collect(Collectors.toList()));
```

#### メソッドチェーンで一気に

```java
List<Person> persons = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .sheetIndex(0)
    .skip(1)
    .headerKey("名前")
    .headerSearchRows(20)
    .extract(stream -> stream
        .filter(p -> p.getAge() >= 25)
        .collect(Collectors.toList()));
```

#### 複数ファイルの読み込み 🔄

複数のExcelファイルを連結して、1つのストリームとして処理できます。メモリ消費を抑えるため、内部的にはファイルを1つずつ順番に処理します。

```java
List<Path> excelFiles = Arrays.asList(
    Paths.get("data1.xlsx"),
    Paths.get("data2.xlsx")
);

// 複数ファイルを連結してストリーム処理
ExcelStreamReader.builder(Person.class, excelFiles)
    .sheetName("Data")           // 全ファイル共通の設定
    .extract(stream -> stream
        .filter(p -> p.getAge() >= 20)
        .collect(Collectors.toList())
    );
```

---

### ExcelReader（一括読み込み）

Streamを使わずに、すべてのデータを一度に読み込んでListとして返すリーダー。シンプルな一括処理に最適！

#### 基本的な使い方

```java
import com.example.excel.reader.ExcelReader;
import com.example.model.Person;
import java.nio.file.Paths;
import java.util.List;

// 基本的な読み込み（extract()/consume()を使わない）
List<Person> persons = ExcelReader.builder(Person.class, Paths.get("sample.xlsx"))
    .read();
```

#### シート指定

```java
// シートインデックスで指定（0から始まる）
List<Person> persons = ExcelReader.builder(Person.class, Paths.get("sample.xlsx"))
    .sheetIndex(0)
    .read();

// シート名で指定
List<Person> persons = ExcelReader.builder(Person.class, Paths.get("sample.xlsx"))
    .sheetName("データ")
    .read();
```

#### ヘッダー行の自動検出

```java
// ヘッダー行を自動検出（上から10行以内で「名前」列を探す）
List<Person> persons = ExcelReader.builder(Person.class, Paths.get("sample.xlsx"))
    .headerKey("名前")
    .read();

// ヘッダー行の探索範囲を20行に拡張
List<Person> persons = ExcelReader.builder(Person.class, Paths.get("sample.xlsx"))
    .headerKey("名前")
    .headerSearchRows(20)
    .read();
```

#### 行のスキップ

```java
// 最初の2行をスキップ（タイトル行などがある場合）
List<Person> persons = ExcelReader.builder(Person.class, Paths.get("sample.xlsx"))
    .skip(2)
    .read();
```

#### ヘッダーなしExcelの読み込み

```java
// 位置ベースのマッピングを使用
List<PersonWithoutHeader> persons = ExcelReader.builder(PersonWithoutHeader.class, Paths.get("no_header.xlsx"))
    .usePositionMapping()
    .read();
```

#### 複数ファイルの読み込み

```java
List<Path> excelFiles = Arrays.asList(
    Paths.get("data1.xlsx"),
    Paths.get("data2.xlsx")
);

// 複数ファイルを読み込んで結合
List<Person> persons = ExcelReader.builder(Person.class, excelFiles)
    .sheetName("Data")           // 全ファイル共通の設定
    .read();
```

**注意**: 大きなファイルの場合は、メモリに全てのデータを保持するため、`ExcelStreamReader`を使用したストリーミング処理を推奨します。

#### Excel列数チェック機能（readWithValidation）

Excelファイルでも列数不一致の行をスキップして最後まで読み込むことができます。

```java
// 列数不一致の行をスキップして最後まで読み込む
ExcelReadResult<Person> result = ExcelReader.builder(Person.class, Paths.get("sample.xlsx"))
    .readWithValidation();

// 成功した行のデータを取得
List<Person> data = result.getData();

// エラー行の情報を取得
List<ExcelReadError> errors = result.getErrors();

// エラーがあるかチェック
if (result.hasErrors()) {
    System.out.println("エラー行数: " + result.getErrorCount());
    errors.forEach(error -> {
        System.out.println("行番号: " + error.getLineNumber() + 
                          ", 期待値: " + error.getExpectedColumnCount() +
                          ", 実際: " + error.getActualColumnCount());
    });
}

// ExcelStreamReaderでも同様に使用可能
ExcelReadResult<Person> result = ExcelStreamReader.builder(Person.class, Paths.get("sample.xlsx"))
    .readWithValidation();
```

**動作:**
- ヘッダー行の列数を基準として設定
- 列数不一致の行はスキップされ、エラー情報として記録されます
- 処理は最後まで続行されます
- 成功した行のデータとエラー行の情報の両方が返されます
- `ExcelStreamReader`でも使用可能です

**注意:** `readWithValidation()`は単一ファイルのみサポートしています。複数ファイルの場合は通常の`read()`メソッドを使用してください。

---

## Excel書き込み機能 📝

Apache POIをラップしたシンプルなExcel書き込みライブラリです。

### ExcelStreamWriter（Stream APIでの書き込み）

Streamを直接書き込めるライター。`ExcelStreamReader`とセットで使うと、ストリーム処理が完結するよ！

#### 基本的な使い方

```java
import com.example.excel.writer.ExcelStreamWriter;
import com.example.model.Person;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

List<Person> persons = Arrays.asList(
    new Person("田中太郎", 25, "エンジニア", "東京"),
    new Person("佐藤花子", 30, "デザイナー", "大阪")
);

// 基本的な書き込み
ExcelStreamWriter.builder(Person.class, Paths.get("output.xlsx"))
    .write(persons.stream());
```

#### シート名の指定

```java
// シート名を指定
ExcelStreamWriter.builder(Person.class, Paths.get("output.xlsx"))
    .sheetName("社員データ")
    .write(persons.stream());
```

#### ヘッダーなしExcelの書き込み

```java
// ヘッダー行を出力しない（データのみ出力）
ExcelStreamWriter.builder(Person.class, Paths.get("no_header.xlsx"))
    .noHeader()
    .write(persons.stream());

// 位置ベースのマッピングと組み合わせて使用
ExcelStreamWriter.builder(PersonWithoutHeader.class, Paths.get("output.xlsx"))
    .usePositionMapping()
    .noHeader()
    .write(persons.stream());
```

#### 既存ファイル（テンプレート）に書き込み

```java
// 既存のExcelファイルを開いて、指定したシートにデータを追加
ExcelStreamWriter.builder(Person.class, Paths.get("template.xlsx"))
    .loadExisting()           // 既存ファイルを読み込む
    .sheetName("データ")       // シート名を指定
    .startCell(2, 0)          // A3セルから書き込み開始（0ベース）
    .write(persons.stream());
```

#### 開始セルの指定

```java
// 指定したセル位置から書き込み開始
ExcelStreamWriter.builder(Person.class, Paths.get("output.xlsx"))
    .startCell(3, 1)  // B4セルから書き込み開始（行: 3, 列: 1）
    .write(persons.stream());
```

#### メソッドチェーンで一気に

```java
ExcelStreamWriter.builder(Person.class, Paths.get("output.xlsx"))
    .sheetName("社員データ")
    .loadExisting()
    .startCell(2, 0)
    .write(persons.stream()
        .filter(p -> p.getAge() >= 30));
```

#### ExcelStreamReaderと組み合わせて使う

```java
// 読み込み → フィルタ → 書き込みの一連の流れ
ExcelStreamReader.builder(Person.class, Paths.get("input.xlsx"))
    .consume(stream -> {
        ExcelStreamWriter.builder(Person.class, Paths.get("output.xlsx"))
            .write(stream.filter(p -> p.getAge() >= 30));
    });
```

---

### ExcelWriter（一括書き込み）

Streamを使わずに、Listを直接書き込むライター。シンプルな一括処理に最適！

#### 基本的な使い方

```java
import com.example.excel.writer.ExcelWriter;
import com.example.model.Person;
import java.nio.file.Paths;
import java.util.List;

List<Person> persons = Arrays.asList(
    new Person("田中太郎", 25, "エンジニア", "東京"),
    new Person("佐藤花子", 30, "デザイナー", "大阪")
);

// 基本的な書き込み（Streamを使わない）
ExcelWriter.builder(Person.class, Paths.get("output.xlsx"))
    .write(persons);
```

#### シート名指定

```java
ExcelWriter.builder(Person.class, Paths.get("output.xlsx"))
    .sheetName("社員データ")
    .write(persons);
```

#### ヘッダーなしExcelの書き込み

```java
// ヘッダー行を出力しない
ExcelWriter.builder(Person.class, Paths.get("no_header.xlsx"))
    .noHeader()
    .write(persons);
```

#### 位置ベースマッピング

```java
ExcelWriter.builder(PersonWithoutHeader.class, Paths.get("output.xlsx"))
    .usePositionMapping()
    .write(persons);
```

#### 既存ファイルへの書き込み

```java
// 既存ファイル（テンプレート）にデータを書き込む
ExcelWriter.builder(Person.class, Paths.get("template.xlsx"))
    .loadExisting()
    .sheetName("データ")
    .startCell(2, 0)  // A3セルから書き込み開始
    .write(persons);
```

#### ExcelReaderと組み合わせて使う

```java
// ExcelReaderで読み込んだデータをExcelWriterで書き込む
List<Person> persons = ExcelReader.builder(Person.class, Paths.get("input.xlsx"))
    .read();

ExcelWriter.builder(Person.class, Paths.get("output.xlsx"))
    .write(persons);
```

**注意**: 大きなファイルの場合は、`ExcelStreamWriter`を使用したストリーミング処理を推奨します。

---

#### CSVStreamReaderと組み合わせて使う（CSV → Excel変換）

```java
import com.example.csv.reader.CsvStreamReader;
import com.example.excel.writer.ExcelStreamWriter;

// CSVから読み込んでExcelに書き込む
CsvStreamReader.builder(Person.class, Paths.get("input.csv"))
    .consume(stream -> {
        ExcelStreamWriter.builder(Person.class, Paths.get("output.xlsx"))
            .sheetName("社員データ")
            .write(stream);
    });

// CSVから読み込んでフィルタしてExcelに書き込む
CsvStreamReader.builder(Person.class, Paths.get("input.csv"))
    .charset(CharsetType.S_JIS)  // Shift_JISのCSVファイル
    .consume(stream -> {
        ExcelStreamWriter.builder(Person.class, Paths.get("output.xlsx"))
            .sheetName("30歳以上")
            .write(stream.filter(p -> p.getAge() >= 30));
    });
```

#### ExcelStreamReaderとCsvStreamWriterを組み合わせて使う（Excel → CSV変換）

```java
import com.example.excel.reader.ExcelStreamReader;
import com.example.csv.writer.CsvStreamWriter;

// Excelから読み込んでCSVに書き込む
ExcelStreamReader.builder(Person.class, Paths.get("input.xlsx"))
    .consume(stream -> {
        CsvStreamWriter.builder(Person.class, Paths.get("output.csv"))
            .charset(CharsetType.UTF_8)
            .write(stream);
    });

// Excelから読み込んでフィルタしてCSVに書き込む
ExcelStreamReader.builder(Person.class, Paths.get("input.xlsx"))
    .sheetName("データ")
    .skip(1)  // タイトル行をスキップ
    .consume(stream -> {
        CsvStreamWriter.builder(Person.class, Paths.get("output.csv"))
            .charset(CharsetType.S_JIS)
            .fileType(FileType.CSV)
            .lineSeparator(LineSeparatorType.CRLF)
            .write(stream.filter(p -> p.getAge() >= 25));
    });
```

### 対応する型

ExcelStreamWriterは以下の型を適切に変換してExcelファイルに書き込みます：

- `String` - 文字列
- `Integer` / `int` - 整数
- `Long` / `long` - 長整数
- `Double` / `double` - 浮動小数点数
- `Boolean` / `boolean` - 真偽値
- `LocalDate` - 日付（自動的に日付フォーマットで書き込まれます）
- `LocalDateTime` - 日時（自動的に日時フォーマットで書き込まれます）
- `Date` - 従来のDate型（自動的に日時フォーマットで書き込まれます）

### 日付型の自動フォーマット

日付型（`LocalDate`, `LocalDateTime`, `Date`）は自動的に適切なExcelの日付/日時フォーマットで書き込まれます。特別な設定は不要です。

---

## BookWriter（DDD設計によるExcel書き込み）📚

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

```java
import com.example.excel.domain.Anchor;

// Anchor値オブジェクトを使用
Anchor anchor = Anchor.of("B5");

Book book = Book.of(Paths.get("output.xlsx"))
    .addSheet(Sheet.of("Test")
        .addTable(Table.builder(Person.class)
            .anchor(anchor)
            .data(persons)
            .build()));

BookWriter.write(book);
```

### Anchor行・列インデックス指定

行と列の数値（0始まり）でAnchorを指定することもできます。

```java
// 行・列インデックス（0始まり）で指定
// row=4, column=1 は B5セル（行5、列B）に対応
Book book = Book.of(Paths.get("output.xlsx"))
    .addSheet(Sheet.of("Test")
        .addTable(Table.builder(Person.class)
            .anchor(4, 1)  // B5セル（0始まり: 行4=5行目、列1=B列）
            .data(persons)
            .build()));

BookWriter.write(book);
```

**注意**: 行・列のインデックスは0始まりです。
- 行0 = 1行目、行4 = 5行目
- 列0 = A列、列1 = B列

### @CsvBindByNameアノテーションからのヘッダー自動抽出

`Table`は`@CsvBindByName`アノテーションから自動的にヘッダーを抽出します。特別な設定は不要です。

```java
// Person Beanクラス（@CsvBindByNameアノテーション付き）
public class Person {
    @CsvBindByName(column = "名前")
    private String name;
    
    @CsvBindByName(column = "年齢")
    private Integer age;
    // ...
}

// Tableは自動的にヘッダーを抽出
Table<Person> table = Table.builder(Person.class)
    .anchor("A1")
    .data(persons)
    .build();
```

### ドメインモデルの特徴

- **`Book`**（エンティティ）: Excelファイル全体を表す
- **`Sheet`**（エンティティ）: Excelシートを表す
- **`Table`**（値オブジェクト）: Excelシート内の1つのテーブル（ブロック）を表す
- **`Anchor`**（値オブジェクト）: Excelセルの位置を表す

詳細は [docs/DDD_DESIGN_EXAMPLE.md](docs/DDD_DESIGN_EXAMPLE.md) を参照してください。

---

## 例外ポリシー

本ライブラリの主な例外方針は以下の通りです。

- Excel 書き込み（`ExcelStreamWriter`）
  - 書き込み時の I/O エラーは **非チェック例外**の `UncheckedIOException` としてスローされます。
  - その他の想定外例外は原因を保持したうえで `UncheckedIOException` に包まれる場合があります。
  - そのため、ラムダ内で `try-catch` は基本的に不要です。

- Excel 読み込み（`ExcelStreamReader` / `ExcelReader`）
  - `extract(...)` / `consume(...)` / `read()` は **`IOException`（チェック例外）** をスローします。呼び出し元で `try-catch` するか、メソッドに `throws IOException` を付与してください。
  - シートやヘッダー関連のドメイン例外（例: `SheetNotFoundException`, `HeaderNotFoundException`, `KeyColumnNotFoundException`）は、状況に応じて非チェック例外としてスローされます。
  - `readWithValidation()` を使用した場合、列数不一致の行はスキップされ、エラー情報として `ExcelReadResult` に含まれます。処理は最後まで続行されます。

- CSV 読み込み（`CsvStreamReader` / `CsvReaderWrapper`）
  - `extract(...)` / `consume(...)` は **`IOException`（チェック例外）** と **`CsvException`（チェック例外）** をスローします。
  - 列数が不一致の場合、**非チェック例外**の `CsvReadException` がスローされます。
  - エラーメッセージには行番号、期待される列数、実際の列数、該当行の内容（プレビュー）が含まれます。
  - `readWithValidation()` を使用した場合、列数不一致の行はスキップされ、エラー情報として `CsvReadResult` に含まれます。処理は最後まで続行されます。

- CSV 書き込み（`CsvStreamWriter` / `CsvWriterWrapper`）
  - 書き込み時のエラーは **非チェック例外**の `CsvWriteException` に変換されます。

- 内部実装での例外ラップ
  - ストリーム処理（ラムダ）内でチェック例外を扱う必要がある箇所では、内部的に `UncheckedExcelException` 等でラップすることがあります。呼び出し側で原因（`getCause()`）をたどれるようになっています。

ガイダンス:
- ストリームの「読み込み側」失敗は外側境界で明確に扱えるようチェック例外を維持。
- 「書き込み側」は呼び出し簡素化のため非チェック化し、必要に応じて上位で一括ハンドリングしてください。

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

### CSV / Excel 共通で使えるOpenCSVアノテーション対応一覧

このライブラリでは、**同じBeanクラスをCSV/Excelの両方で再利用できる**ように、OpenCSVのアノテーションを可能な限り共通サポートしています。

| アノテーション                         | 役割                             | CSV (`CsvStreamReader` / `CsvReaderWrapper`) | Excel (`ExcelStreamReader` / `ExcelReader`) |
| -------------------------------------- | -------------------------------- | ------------------------------------------- | ------------------------------------------- |
| `@CsvBindByName`                       | ヘッダー名でフィールドをバインド | ✅ 対応                                      | ✅ 対応                                      |
| `@CsvBindByPosition`                   | 列位置でフィールドをバインド     | ✅ 対応                                      | ✅ 対応                                      |
| `@PreAssignmentValidator`              | 代入前バリデーション             | ✅ 対応（OpenCSV標準）                       | ✅ 対応（セル値→フィールド代入前に実行）    |
| `@CsvCustomBindByName`                 | 名前ベースのカスタム変換         | ✅ 対応（OpenCSV標準）                       | ✅ 対応（事前変換として実行）               |
| `@CsvCustomBindByPosition`             | 位置ベースのカスタム変換         | ✅ 対応（OpenCSV標準）                       | ✅ 対応（事前変換として実行）               |
| `@CsvDate`                             | 日付フォーマット指定             | ✅ 対応（CSV側のOpenCSV機能）               | ❌ 直接サポートなし（Excel側はセル型＋`CellValueConverter`で処理） |
| `@CsvNumber`                           | 数値フォーマット指定             | ✅ 対応（CSV側のOpenCSV機能）               | ❌ 直接サポートなし                          |

> ✅: このライブラリとして明示的にサポート / 統合しているもの  
> ❌: Excel側では直接は解釈せず、セル型や独自ロジックで処理するもの

### 事前バリデーション (@PreAssignmentValidator)

`@PreAssignmentValidator` で指定したバリデータは、**文字列がフィールドに代入される前**に実行されます。

```java
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.validators.PreAssignmentValidator;

public class Person {

    @CsvBindByName(column = "年齢")
    @PreAssignmentValidator(validator = AgeValidator.class)
    private Integer age;
}
```

Excel 読み込みでも、各セルから文字列を取得したあと、型変換やフィールド代入の前に同じバリデーションが適用されます。

### 事前変換（前処理）: @CsvCustomBindByName / @CsvCustomBindByPosition

OpenCSVのカスタムバインドアノテーション：

- `@CsvCustomBindByName`
- `@CsvCustomBindByPosition`

で指定した `AbstractBeanField` ベースのコンバーターも、**CSV と同じ感覚で Excel 読み込み時に実行**されます。

```java
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import com.opencsv.bean.AbstractBeanField;

public class Person {

    // 名前列をトリム＋大文字化
    @CsvCustomBindByName(column = "名前", converter = TrimUpperNameConverter.class)
    private String name;

    // 位置ベースで文字列→Integerに変換
    @CsvCustomBindByPosition(position = 1, converter = TrimToIntConverter.class)
    private Integer age;
}

public class TrimUpperNameConverter extends AbstractBeanField<String, String> {
    @Override
    protected String convert(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}

public class TrimToIntConverter extends AbstractBeanField<Integer, String> {
    @Override
    protected Integer convert(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Integer.parseInt(value.trim());
    }
}
```

これらのコンバーターは、

1. セルから文字列を取得
2. `@PreAssignmentValidator` による事前バリデーション（あれば）
3. `convert(...)` による前処理（カスタム変換）
4. フィールドへの代入

という順序で適用されます（CSV / Excel 共通）。

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
