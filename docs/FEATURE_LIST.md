# 📋 Excel RW Wrapper 機能一覧

このドキュメントは、Excel RW Wrapperライブラリの全機能を一覧化したものです。各機能の詳細なサンプルコードは[README.md](../README.md)を参照してください。

## 📖 目次

1. [CSV読み込み機能](#csv読み込み機能)
2. [CSV書き込み機能](#csv書き込み機能)
3. [Excel読み込み機能](#excel読み込み機能)
4. [Excel書き込み機能](#excel書き込み機能)
5. [その他の機能](#その他の機能)

---

## CSV読み込み機能

### CsvReaderWrapper（通常版・推奨）

**一括読み込み方式。すべてのデータをメモリに読み込んでListとして返します。**

#### 主な機能

- ✅ 基本的なCSV/TSV読み込み
- ✅ 文字セット指定（UTF-8、Shift_JIS、EUC-JP、Windows-31Jなど）
- ✅ ファイルタイプ指定（CSV/TSV）
- ✅ 行スキップ機能
- ✅ ヘッダーあり/なし対応
- ✅ 位置ベースマッピング対応
- ✅ 行番号トラッキング機能
- ✅ 列数検証機能（自動検出）
- ✅ 列数不一致でも最後まで読み込む機能（`readWithValidation()`）
- ✅ 複数ファイルの並列読み込み

#### サンプルコード

詳細なサンプルコードは [README.md - CsvReaderWrapper（推奨）](../README.md#csvreaderwrapper推奨) を参照してください。

---

### CsvStreamReader（ストリーム版）

**Stream APIでの読み込み。メモリ効率の良い逐次処理が可能です。**

#### 主な機能

- ✅ Stream APIでの読み込み
- ✅ 文字セット指定
- ✅ ファイルタイプ指定（CSV/TSV）
- ✅ 行スキップ機能
- ✅ ヘッダーあり/なし対応
- ✅ 位置ベースマッピング対応
- ✅ フィルタ/マップなどのStream操作
- ✅ 列数検証機能（自動検出）
- ✅ 列数不一致でも最後まで読み込む機能（`readWithValidation()`）

#### サンプルコード

詳細なサンプルコードは [README.md - CsvStreamReader（Stream APIでの読み込み）](../README.md#csvstreamreaderstream-apiでの読み込み) を参照してください。

---

## CSV書き込み機能

### CsvWriterWrapper（通常版・推奨）

**一括書き込み方式。Listを直接CSVファイルに書き込みます。**

#### 主な機能

- ✅ 基本的なCSV/TSV書き込み
- ✅ 文字セット指定
- ✅ ファイルタイプ指定（CSV/TSV）
- ✅ 改行コード指定（CRLF、LF、CR）
- ✅ ヘッダーあり/なし制御
- ✅ 位置ベースマッピング対応
- ✅ クオート戦略指定

#### サンプルコード

詳細なサンプルコードは [README.md - CsvWriterWrapper（推奨）](../README.md#csvwriterwrapper推奨) を参照してください。

---

### CsvStreamWriter（ストリーム版）

**Stream APIでの書き込み。Streamを直接CSVファイルに書き込みます。**

#### 主な機能

- ✅ Stream APIでの書き込み
- ✅ 文字セット指定
- ✅ ファイルタイプ指定（CSV/TSV）
- ✅ 改行コード指定
- ✅ ヘッダーあり/なし制御
- ✅ 位置ベースマッピング対応
- ✅ クオート戦略指定
- ✅ CsvStreamReaderと組み合わせて使用可能

#### サンプルコード

詳細なサンプルコードは [README.md - CsvStreamWriter（Stream APIでの書き込み）](../README.md#csvstreamwriterstream-apiでの書き込み) を参照してください。

---

## Excel読み込み機能

### ExcelReader（通常版）

**一括読み込み方式。すべてのデータをメモリに読み込んでListとして返します。**

#### 主な機能

- ✅ 基本的なExcel読み込み
- ✅ シート指定（インデックス/名前）
- ✅ ヘッダー行の自動検出
- ✅ 行スキップ機能
- ✅ ヘッダーあり/なし対応
- ✅ 位置ベースマッピング対応
- ✅ 複数ファイルの読み込み
- ✅ 列数不一致でも最後まで読み込む機能（`readWithValidation()`）

#### サンプルコード

詳細なサンプルコードは [README.md - ExcelReader（一括読み込み）](../README.md#excelreader一括読み込み) を参照してください。

---

### ExcelStreamReader（ストリーム版）

**Stream APIでの読み込み。メモリ効率の良いストリーミング処理が可能です。**

#### 主な機能

- ✅ Stream APIでの読み込み
- ✅ シート指定（インデックス/名前）
- ✅ ヘッダー行の自動検出
- ✅ 行スキップ機能
- ✅ ヘッダーあり/なし対応
- ✅ 位置ベースマッピング対応
- ✅ フィルタ/マップなどのStream操作
- ✅ 複数ファイルの連結読み込み
- ✅ 列数不一致でも最後まで読み込む機能（`readWithValidation()`）

#### サンプルコード

詳細なサンプルコードは [README.md - ExcelStreamReader（Stream APIでの読み込み）](../README.md#excelstreamreaderstream-apiでの読み込み) を参照してください。

---

## Excel書き込み機能

### ExcelWriter（通常版）

**一括書き込み方式。Listを直接Excelファイルに書き込みます。**

#### 主な機能

- ✅ 基本的なExcel書き込み
- ✅ シート名指定
- ✅ ヘッダーあり/なし制御
- ✅ 位置ベースマッピング対応
- ✅ 既存ファイルへの書き込み（テンプレート対応）
- ✅ 開始セル位置指定

#### サンプルコード

詳細なサンプルコードは [README.md - ExcelWriter（一括書き込み）](../README.md#excelwriter一括書き込み) を参照してください。

---

### ExcelStreamWriter（ストリーム版）

**Stream APIでの書き込み。Streamを直接Excelファイルに書き込みます。**

#### 主な機能

- ✅ Stream APIでの書き込み
- ✅ シート名指定
- ✅ ヘッダーあり/なし制御
- ✅ 位置ベースマッピング対応
- ✅ 既存ファイルへの書き込み（テンプレート対応）
- ✅ 開始セル位置指定
- ✅ ExcelStreamReaderと組み合わせて使用可能
- ✅ CsvStreamReaderと組み合わせて使用可能（CSV → Excel変換）

#### サンプルコード

詳細なサンプルコードは [README.md - ExcelStreamWriter（Stream APIでの書き込み）](../README.md#excelstreamwriterstream-apiでの書き込み) を参照してください。

---

### BookWriter（DDD設計によるExcel書き込み）

**DDD（ドメイン駆動設計）的な設計に基づき、`Book`、`Sheet`、`Table`のドメインモデルを使用してExcelファイルに書き込みます。**

#### 主な機能

- ✅ DDD設計パターンによるExcel書き込み
- ✅ 複数シートの書き込み
- ✅ 複数テーブルの配置
- ✅ 既存ファイルへの追記
- ✅ Anchor値オブジェクトによる位置指定
- ✅ 行・列インデックスによる位置指定

#### サンプルコード

詳細なサンプルコードは [README.md - BookWriter（DDD設計によるExcel書き込み）](../README.md#bookwriterddd設計によるexcel書き込み) および [docs/DDD_DESIGN_EXAMPLE.md](DDD_DESIGN_EXAMPLE.md) を参照してください。

---

## その他の機能

### CsvExternalSorter（大容量CSV外部ソート）

**4GB～10GB程度の大きなCSVファイルをメモリに収まらなくても効率的にソートできます。**

#### 主な機能

- ✅ メモリ効率の良い外部ソート
- ✅ 任意の列や複数列でのソート
- ✅ k-wayマージソートアルゴリズム
- ✅ 自動クリーンアップ（一時ファイル削除）
- ✅ チャンクサイズ指定
- ✅ 文字セット指定
- ✅ ヘッダー行スキップ

#### サンプルコード

詳細なサンプルコードは [README.md - 大容量CSV外部ソート機能](../README.md#大容量csv外部ソート機能-) および [docs/EXTERNAL_SORT_USAGE.md](EXTERNAL_SORT_USAGE.md) を参照してください。

---

## 共通機能

### 行番号トラッキング機能

CSV/Excelの読み込み時に、データの元ファイル行番号を自動的に取得できます。

#### 実装方法

1. **抽象クラス継承**（最も簡単・推奨）
2. **インターフェース実装**
3. **アノテーションのみ**
4. **RowDataラッパー**（既存モデルを変更したくない場合）

詳細なサンプルコードは [README.md - 行番号トラッキング機能](../README.md#行番号トラッキング機能-) を参照してください。

---

### 列数検証機能

CSV/TSVファイルを読み込む前に、自動的に列数の整合性をチェックします。

#### 主な機能

- ✅ 列数不一致の自動検出
- ✅ エラーメッセージに行番号、期待値、実際の値、行内容を含む
- ✅ CSV/TSVの両方で動作
- ✅ ダブルクォート内の区切り文字を正しく処理

詳細なサンプルコードは [README.md - 列数検証機能](../README.md#列数検証機能) を参照してください。

---

### 列数不一致でも最後まで読み込む機能

列数が不一致の行があっても処理を止めず、最後まで読み込んでエラー行の情報を取得できます。

#### 対応クラス

- `CsvReaderWrapper.readWithValidation()`
- `CsvStreamReader.readWithValidation()`
- `ExcelReader.readWithValidation()`
- `ExcelStreamReader.readWithValidation()`

詳細なサンプルコードは [README.md - 列数不一致でも最後まで読み込む機能](../README.md#列数不一致でも最後まで読み込む機能readwithvalidation) を参照してください。

---

### アノテーション対応

OpenCSVのアノテーションをCSV/Excelの両方で共通利用できます。

#### 対応アノテーション

- `@CsvBindByName` - ヘッダー名でフィールドをバインド
- `@CsvBindByPosition` - 列位置でフィールドをバインド
- `@PreAssignmentValidator` - 代入前バリデーション
- `@CsvCustomBindByName` - 名前ベースのカスタム変換
- `@CsvCustomBindByPosition` - 位置ベースのカスタム変換
- `@CsvDate` - 日付フォーマット指定（CSVのみ）
- `@CsvNumber` - 数値フォーマット指定（CSVのみ）

詳細なサンプルコードは [README.md - アノテーションでの項目名指定](../README.md#アノテーションでの項目名指定) を参照してください。

---

## ファイル形式対応

### CSV/TSV

- ✅ CSV（カンマ区切り）
- ✅ TSV（タブ区切り）

### Excel

- ✅ XLSX形式（.xlsx）

---

## 文字エンコーディング対応

以下の文字エンコーディングに対応しています：

- ✅ UTF-8（BOM有り/無し）
- ✅ Shift_JIS
- ✅ EUC-JP
- ✅ Windows-31J

---

## マッピング方式

### ヘッダーベースマッピング（デフォルト）

ヘッダー行の名前でフィールドをマッピングします。`@CsvBindByName`アノテーションを使用します。

### 位置ベースマッピング

カラムの位置でフィールドをマッピングします。ヘッダーなしCSV/Excel用です。`@CsvBindByPosition`アノテーションを使用します。

---

## ベストプラクティス

### ストリーミング処理

大量データをメモリ効率よく処理する方法については、[docs/STREAMING_BEST_PRACTICES.md](STREAMING_BEST_PRACTICES.md) を参照してください。

### グルーピング処理

グルーピング処理でメモリを節約する方法については、[docs/GROUPING_BEST_PRACTICES.md](GROUPING_BEST_PRACTICES.md) を参照してください。

### 大量データ処理

5GB以上の大容量ファイルを処理する方法については、[docs/LARGE_DATA_GROUPING_USAGE.md](LARGE_DATA_GROUPING_USAGE.md) を参照してください。

---

## 関連ドキュメント

- [README.md](../README.md) - 完全なAPIリファレンスとサンプルコード
- [docs/GUIDE.md](GUIDE.md) - 完全ガイド
- [docs/STREAMING_BEST_PRACTICES.md](STREAMING_BEST_PRACTICES.md) - ストリーミング処理のベストプラクティス
- [docs/GROUPING_BEST_PRACTICES.md](GROUPING_BEST_PRACTICES.md) - グルーピング処理のベストプラクティス
- [docs/LARGE_DATA_GROUPING_USAGE.md](LARGE_DATA_GROUPING_USAGE.md) - 大量データグルーピング＆ソート機能
- [docs/EXTERNAL_SORT_USAGE.md](EXTERNAL_SORT_USAGE.md) - CSV外部ソート機能
- [docs/DDD_DESIGN_EXAMPLE.md](DDD_DESIGN_EXAMPLE.md) - DDD設計によるExcel書き込み
- [docs/MIGRATION.md](MIGRATION.md) - 移行ガイド
