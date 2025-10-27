# CsvReaderWrapper リファクタリング移行ガイド

## 📋 目次
- [概要](#概要)
- [変更内容](#変更内容)
- [移行方法](#移行方法)
- [新旧API対応表](#新旧api対応表)
- [移行例](#移行例)
- [FAQ](#faq)
- [サポート](#サポート)

---

## 📌 概要

`CsvReaderWrapper` が新しいBuilderパターンを導入してリファクタリングされました。
**既存のコードは完全に互換性を維持しており、すぐに動作しなくなることはありません。**

しかし、新しいBuilderパターンはより直感的で読みやすいため、今後の開発では新しいAPIの使用を推奨します。

### リファクタリングの目的
- ✅ コードの可読性向上
- ✅ より直感的なAPI設計
- ✅ メソッド責任の明確化
- ✅ エラーハンドリングの改善
- ✅ ユーティリティクラスの再利用性向上

---

## 🔄 変更内容

### 1. 新しいBuilderパターンの追加

従来の `execute()` メソッドに加え、新しい `builder()` メソッドが追加されました。

#### Before (従来のAPI - 引き続き使用可能)
```java
List<Person> persons = CsvReaderWrapper.execute(
    Person.class,
    Paths.get("sample.csv"),
    instance -> instance.setCharset(CharsetType.UTF_8_BOM).setSkip(1).read()
);
```

#### After (新しいAPI - 推奨)
```java
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("sample.csv"))
    .charset(CharsetType.UTF_8_BOM)
    .skipLines(1)
    .read();
```

### 2. 新しいクラスの追加

#### `CsvReadException`
CSV読み込み時の専用例外クラスが追加されました。
従来の `RuntimeException` よりも明確なエラーハンドリングが可能になります。

```java
try {
    List<Person> persons = CsvReaderWrapper.builder(Person.class, path).read();
} catch (CsvReadException e) {
    // CSV読み込みエラーの処理
    log.error("CSV読み込みエラー: {}", e.getMessage());
}
```

#### `BomSkipper`
BOM (Byte Order Mark) スキップ処理が独立したユーティリティクラスになりました。
他のクラスでも再利用可能です。

```java
InputStream is = BomSkipper.skip(fileInputStream);
```

### 3. 内部リファクタリング

- `createMappingStrategy()`: マッピング戦略の生成を独立したメソッドに分離
- `applySkipLines()`: スキップ行処理を独立したメソッドに分離
- `StandardCharsets.UTF_8` の使用: 型安全な文字セット定義に変更

---

## 🚀 移行方法

### ステップ1: 既存コードの動作確認

まず、現在のコードがそのまま動作することを確認してください。

```bash
# テストを実行
./gradlew test

# または Windows の場合
gradlew.bat test
```

### ステップ2: 新しいAPIへの段階的移行

**重要: 一度にすべてを変更する必要はありません。**
プロジェクトの開発サイクルに合わせて、段階的に移行してください。

#### 推奨移行順序

1. **新機能・新規実装**: 新しいBuilderパターンを使用
2. **既存コードの修正時**: 該当部分を新しいAPIに移行
3. **リファクタリング期間**: 既存コード全体を徐々に移行

### ステップ3: 例外ハンドリングの更新（オプション）

より明確なエラーハンドリングのため、`CsvReadException` のキャッチを推奨します。

```java
// 従来
try {
    List<Person> persons = CsvReaderWrapper.execute(...);
} catch (IOException | CsvException e) {
    // エラー処理
}

// 推奨（新API）
try {
    List<Person> persons = CsvReaderWrapper.builder(...).read();
} catch (CsvReadException e) {
    // CSV固有のエラー処理
}
```

---

## 📊 新旧API対応表

### メソッド名の変更

| 従来のAPI | 新しいAPI | 説明 |
|-----------|-----------|------|
| `execute()` | `builder()` | エントリーポイント |
| `setSkip(n)` | `skipLines(n)` | より明確な命名 |
| `setCharset(type)` | `charset(type)` | より簡潔な命名 |
| `setFileType(type)` | `fileType(type)` | より簡潔な命名 |
| `usePositionMapping()` | `usePositionMapping()` | 変更なし |
| `useHeaderMapping()` | `useHeaderMapping()` | 変更なし |

---

## 💡 移行例

### 例1: 基本的な読み込み

#### Before
```java
List<Person> persons = CsvReaderWrapper.execute(
    Person.class,
    Paths.get("src/test/resources/sample.csv"),
    instance -> instance.read()
);
```

#### After
```java
List<Person> persons = CsvReaderWrapper.builder(
    Person.class,
    Paths.get("src/test/resources/sample.csv")
).read();
```

### 例2: スキップ行の設定

#### Before
```java
List<Person> persons = CsvReaderWrapper.execute(
    Person.class,
    Paths.get("sample.csv"),
    instance -> instance.setSkip(1).read()
);
```

#### After
```java
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("sample.csv"))
    .skipLines(1)
    .read();
```

### 例3: 文字セットの設定

#### Before
```java
List<Person> persons = CsvReaderWrapper.execute(
    Person.class,
    Paths.get("sample_sjis.csv"),
    instance -> instance.setCharset(CharsetType.S_JIS).read()
);
```

#### After
```java
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("sample_sjis.csv"))
    .charset(CharsetType.S_JIS)
    .read();
```

### 例4: TSVファイルの読み込み

#### Before
```java
List<Person> persons = CsvReaderWrapper.execute(
    Person.class,
    Paths.get("sample.tsv"),
    instance -> instance.setFileType(FileType.TSV).read()
);
```

#### After
```java
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("sample.tsv"))
    .fileType(FileType.TSV)
    .read();
```

### 例5: 位置ベースマッピング

#### Before
```java
List<Person2> persons = CsvReaderWrapper.execute(
    Person2.class,
    Paths.get("sample_no_header.csv"),
    instance -> instance.usePositionMapping().read()
);
```

#### After
```java
List<Person2> persons = CsvReaderWrapper.builder(Person2.class, Paths.get("sample_no_header.csv"))
    .usePositionMapping()
    .read();
```

### 例6: BOM付きUTF-8ファイル

#### Before
```java
List<Person> persons = CsvReaderWrapper.execute(
    Person.class,
    Paths.get("sample_utf8_bom.csv"),
    instance -> instance.setCharset(CharsetType.UTF_8_BOM).read()
);
```

#### After
```java
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("sample_utf8_bom.csv"))
    .charset(CharsetType.UTF_8_BOM)
    .read();
```

### 例7: 複数設定の組み合わせ

#### Before
```java
List<Person> persons = CsvReaderWrapper.execute(
    Person.class,
    Paths.get("sample.csv"),
    instance -> instance
        .setSkip(1)
        .setCharset(CharsetType.UTF_8)
        .setFileType(FileType.CSV)
        .useHeaderMapping()
        .read()
);
```

#### After
```java
List<Person> persons = CsvReaderWrapper.builder(Person.class, Paths.get("sample.csv"))
    .skipLines(1)
    .charset(CharsetType.UTF_8)
    .fileType(FileType.CSV)
    .useHeaderMapping()
    .read();
```

---

## ❓ FAQ

### Q1: 既存のコードはすぐに変更する必要がありますか？

**A:** いいえ、必要ありません。従来の `execute()` メソッドは完全に互換性を維持しており、引き続き使用できます。ただし、新しいコードでは `builder()` メソッドの使用を推奨します。

### Q2: いつまでに移行する必要がありますか？

**A:** 明確な期限はありません。プロジェクトの開発サイクルに合わせて、段階的に移行することを推奨します。ただし、次のメジャーバージョンアップ時に `execute()` メソッドが非推奨（@Deprecated）となる可能性があります。

### Q3: 新旧APIを混在させても問題ありませんか？

**A:** はい、問題ありません。同一プロジェクト内で両方のAPIを使用できます。

### Q4: テストコードも移行する必要がありますか？

**A:** テストコードは既存のまま動作しますが、新しいテストでは新しいAPIを使用することを推奨します。

### Q5: パフォーマンスに違いはありますか？

**A:** いいえ、内部処理は同じため、パフォーマンスに違いはありません。

### Q6: エラーハンドリングはどう変わりますか？

**A:** 新しいAPIでは `CsvReadException` がスローされます。これにより、より明確なエラーハンドリングが可能になります。ただし、`CsvReadException` は `RuntimeException` を継承しているため、キャッチしなくても動作します。

### Q7: BomSkipperを直接使用できますか？

**A:** はい、`BomSkipper.skip(InputStream)` を直接使用できます。CSV読み込み以外の場面でもBOMスキップが必要な場合に便利です。

---

## 🛠️ サポート

### トラブルシューティング

#### ケース1: コンパイルエラーが発生する

```
error: cannot find symbol
  symbol:   method builder(Class<Person>, Path)
```

**解決方法**: プロジェクトを再ビルドしてください。

```bash
./gradlew clean build
```

#### ケース2: CsvReadException が認識されない

```
error: cannot find symbol
  symbol:   class CsvReadException
```

**解決方法**: インポート文を追加してください。

```java
import com.example.csv.CsvReadException;
```

#### ケース3: 既存のテストが失敗する

**解決方法**: リファクタリングは互換性を維持しているため、既存のテストが失敗することはありません。失敗する場合は、他の要因（ファイルパス、テストデータなど）を確認してください。

### 質問・問題報告

リファクタリングに関する質問や問題がある場合は、以下の方法でお問い合わせください：

- **GitHub Issues**: プロジェクトのIssuesページで質問・問題を報告
- **Pull Request**: 改善提案は Pull Request でお願いします

---

## 📝 変更履歴

### Version 2.0.0 (Current)

#### 新機能
- ✨ Builderパターンの導入（`builder()` メソッド）
- ✨ `CsvReadException` カスタム例外の追加
- ✨ `BomSkipper` ユーティリティクラスの追加

#### 改善
- 🔨 内部メソッドのリファクタリング（`createMappingStrategy()`, `applySkipLines()`）
- 🔨 `StandardCharsets` の使用による型安全性の向上
- 📚 JavaDocの充実

#### 互換性
- ✅ 従来の `execute()` メソッドは完全互換で継続サポート
- ✅ 既存コードの変更不要

---

## 🎯 推奨移行スケジュール

### フェーズ1: 評価期間（1-2週間）
- ✅ 新しいAPIの動作確認
- ✅ サンプルコードでの試験的使用
- ✅ チーム内での情報共有

### フェーズ2: 部分的移行（1-2ヶ月）
- ✅ 新機能で新しいAPIを使用
- ✅ バグ修正時に該当箇所を移行
- ✅ コードレビューでの確認

### フェーズ3: 全面移行（必要に応じて）
- ✅ 既存コード全体のリファクタリング
- ✅ レガシーAPIの削除検討

---

## 💖 まとめ

このリファクタリングは、コードの保守性と可読性を向上させるために行われました。
**既存のコードはそのまま動作します**ので、焦らず段階的に移行してください。

新しいBuilderパターンは、以下の点で優れています：

1. **直感的**: メソッド名が明確で理解しやすい
2. **読みやすい**: メソッドチェーンが自然な流れ
3. **保守しやすい**: 設定の追加・変更が容易
4. **型安全**: より厳格な型チェック

ご不明な点がございましたら、お気軽にお問い合わせください！

---

**Happy Coding! 🚀**

