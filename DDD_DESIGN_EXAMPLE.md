# DDD設計によるExcel書き込みAPI

## 概要

DDD（ドメイン駆動設計）的な設計に基づき、`Book`、`Sheet`、`Table`のドメインモデルを作成しました。
この設計により、Excelファイルの構造をドメインモデルとして表現し、ビジネスロジックをドメインモデルに集約できます。

## ドメインモデル

### Book（エンティティ）
Excelファイル全体を表すドメインオブジェクト。
- ファイルパスを保持
- 複数の`Sheet`を含むことができる
- 既存ファイルの読み込み設定を保持

### Sheet（エンティティ）
Excelシートを表すドメインオブジェクト。
- シート名を保持
- 複数の`Table`を含むことができる
- テーブルの追加・削除などの操作を提供

### Table（値オブジェクト）
Excelシート内の1つのテーブル（ブロック）を表すドメインオブジェクト。
- Beanクラス、タイトル、アンカーセル、データを保持
- `@CsvBindByName`アノテーションからヘッダーを自動抽出
- 不変性を保証

### Anchor（値オブジェクト）
Excelセルの位置を表す値オブジェクト。
- アンカーセル文字列（例: "A1"）を解析して行・列インデックスを保持
- 不変性を保証

## 使用例

### 基本的な使用例

```java
// Person Beanクラス（@CsvBindByNameアノテーション付き）
public class Person {
    @CsvBindByName(column = "名前")
    private String name;
    @CsvBindByName(column = "年齢")
    private Integer age;
    // ...
}

// Order Beanクラス（@CsvBindByNameアノテーション付き）
public class Order {
    @CsvBindByName(column = "注文ID")
    private String orderId;
    // ...
}

// ドメインモデルを構築
Book book = Book.of(Paths.get("output.xlsx"))
    .addSheet(Sheet.of("Report")
        .addTable(Table.builder(Person.class)
            .title("# Users")
            .anchor("A1")
            .data(users)
            .build())
        .addTable(Table.builder(Order.class)
            .title("# Orders")
            .anchor("A20")
            .data(orders)
            .build()));

// 書き込み
BookWriter.write(book);
```

### 複数シートの例

```java
Book book = Book.of(Paths.get("output.xlsx"))
    .addSheet(Sheet.of("Users")
        .addTable(Table.builder(Person.class)
            .title("# Users")
            .anchor("A1")
            .data(users)
            .build()))
    .addSheet(Sheet.of("Orders")
        .addTable(Table.builder(Order.class)
            .title("# Orders")
            .anchor("A1")
            .data(orders)
            .build()));

BookWriter.write(book);
```

### 既存ファイルに追記する例

```java
Book book = Book.of(Paths.get("template.xlsx"))
    .withLoadExisting()
    .addSheet(Sheet.of("Report")
        .addTable(Table.builder(Person.class)
            .title("# Users")
            .anchor("A1")
            .data(users)
            .build()));

BookWriter.write(book);
```

## 設計のメリット

### 1. ドメインモデルが明確
- `Book`、`Sheet`、`Table`の関係が明確に表現される
- ビジネスロジックをドメインモデルに集約できる

### 2. 再利用性が高い
- `Sheet`や`Table`を独立してテスト・再利用可能
- ドメインモデルを組み合わせて複雑な構造を表現できる

### 3. 拡張性が高い
- Sheet間の関係性を表現できる
- Tableの依存関係を表現できる
- 将来的な機能追加（例: テーブル間の参照、バリデーション）に対応しやすい

### 4. テストしやすい
- 各ドメインオブジェクトを個別にテスト可能
- モックを使ったテストが容易

### 5. 不変性の保証
- `Table`や`Anchor`は値オブジェクトとして設計され、不変性を保証
- 副作用を防ぎ、バグの発生を抑制

## 設計のデメリット

### 1. オーバーエンジニアリングの可能性
- シンプルなユースケースには複雑すぎる可能性がある
- 小規模なプロジェクトでは過剰な設計になる可能性がある

### 2. 学習コスト
- DDDの概念を理解する必要がある
- 新しいチームメンバーへの説明が必要

### 3. コード量の増加
- ドメインモデルとインフラストラクチャ層を分離するため、コード量が増える
- メンテナンスコストが増える可能性がある

## まとめ

DDD的な設計は、複雑なExcel構造を扱う場合や、長期的なメンテナンスが必要な場合に有効です。

