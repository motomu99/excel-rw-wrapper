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

### 基本的なBean読み込み

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

public class Person {
    @CsvBindByName(column = "名前")
    private String name;
    
    @CsvBindByName(column = "年齢")
    private Integer age;
    
    @CsvBindByName(column = "職業")
    private String occupation;
    
    @CsvBindByName(column = "出身地")
    private String birthplace;
    
    // Getter/Setter
    // ...
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

## ライセンス

MIT License
