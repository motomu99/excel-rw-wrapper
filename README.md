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

### 基本的な使い方

```java
import com.example.csv.CsvReader;
import java.util.List;

// CsvReaderのインスタンスを作成
CsvReader csvReader = new CsvReader();

// CSVファイルを読み込み
List<String[]> data = csvReader.readCsvFile("path/to/your/file.csv");

// データを処理
for (String[] row : data) {
    for (String cell : row) {
        System.out.print(cell + " ");
    }
    System.out.println();
}
```

### ヘッダーを除いたデータのみを取得

```java
// ヘッダー行を除いたデータのみを取得
List<String[]> dataOnly = csvReader.readCsvDataOnly("path/to/your/file.csv", true);
```

### ヘッダー情報のみを取得

```java
// ヘッダー行のみを取得
String[] header = csvReader.readCsvHeader("path/to/your/file.csv");
```

### InputStreamから読み込み

```java
// InputStreamからCSVを読み込み
List<String[]> data = csvReader.readCsvFromStream(inputStream);
```

## Bean読み込み機能

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

---

## CSV書き込み機能

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

### CsvBeanReader（レガシー）

```java
import com.example.csv.CsvBeanReader;
import com.example.csv.model.Person;
import java.util.List;

// CsvBeanReaderのインスタンスを作成
CsvBeanReader csvBeanReader = new CsvBeanReader();

// CSVファイルをBeanのListとして読み込み
List<Person> persons = csvBeanReader.readCsvToBeans("path/to/your/file.csv", Person.class);

// Beanのプロパティにアクセス
for (Person person : persons) {
    System.out.println("名前: " + person.getName());
    System.out.println("年齢: " + person.getAge());
}
```

### アノテーションでの項目名指定

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

### InputStreamからBean読み込み

```java
// InputStreamからBeanを読み込み
List<Person> persons = csvBeanReader.readCsvToBeansFromStream(inputStream, Person.class);
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

- **CsvReaderクラス**: 73%のカバレッジ
- **ブランチカバレッジ**: 66%
- **メソッドカバレッジ**: 80%

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
