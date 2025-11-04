/**
 * CSV/TSVファイルの読み書きを簡単に行うためのラッパーライブラリ
 *
 * <p>このパッケージは、OpenCSVライブラリをベースに、より使いやすいAPIを提供します。
 * Builderパターンを採用し、柔軟な設定でCSV/TSVファイルの読み書きが可能です。</p>
 *
 * <h2>主要クラス</h2>
 *
 * <h3>CSV読み込み</h3>
 * <ul>
 *   <li>{@link com.example.csv.reader.CsvReaderWrapper} - CSV/TSVファイルを一括読み込み</li>
 *   <li>{@link com.example.csv.reader.CsvStreamReader} - CSV/TSVファイルをStream APIで処理</li>
 * </ul>
 *
 * <h3>CSV書き込み</h3>
 * <ul>
 *   <li>{@link com.example.csv.writer.CsvWriterWrapper} - CSV/TSVファイルに一括書き込み</li>
 *   <li>{@link com.example.csv.writer.CsvStreamWriter} - Stream APIを使ってCSV/TSVファイルに書き込み</li>
 * </ul>
 *
 * <h3>設定用列挙型</h3>
 * <ul>
 *   <li>{@link com.example.common.config.CharsetType} - 文字エンコーディングの指定</li>
 *   <li>{@link com.example.common.config.FileType} - ファイル形式（CSV/TSV）の指定</li>
 *   <li>{@link com.example.common.config.LineSeparatorType} - 改行コードの指定</li>
 * </ul>
 *
 * <h3>例外クラス</h3>
 * <ul>
 *   <li>{@link com.example.exception.CsvReadException} - CSV読み込み時の例外</li>
 *   <li>{@link com.example.exception.CsvWriteException} - CSV書き込み時の例外</li>
 * </ul>
 *
 * <h2>基本的な使い方</h2>
 *
 * <h3>CSVファイルの読み込み</h3>
 * <pre>
 * // Beanクラスの定義
 * public class Person {
 *     {@literal @}CsvBindByName
 *     private String name;
 *     {@literal @}CsvBindByName
 *     private int age;
 *     // getters and setters
 * }
 *
 * // CSV読み込み
 * List&lt;Person&gt; persons = CsvReaderWrapper.builder(Person.class, Paths.get("data.csv"))
 *     .charset(CharsetType.UTF_8_BOM)
 *     .read();
 * </pre>
 *
 * <h3>CSVファイルの書き込み</h3>
 * <pre>
 * List&lt;Person&gt; persons = Arrays.asList(
 *     new Person("田中", 25),
 *     new Person("佐藤", 30)
 * );
 *
 * CsvWriterWrapper.builder(Person.class, Paths.get("output.csv"))
 *     .charset(CharsetType.UTF_8_BOM)
 *     .lineSeparator(LineSeparatorType.LF)
 *     .write(persons);
 * </pre>
 *
 * <h3>Stream APIを使った処理</h3>
 * <pre>
 * // 読み込み時にフィルタリング
 * List&lt;Person&gt; adults = CsvStreamReader.builder(Person.class, Paths.get("data.csv"))
 *     .charset(CharsetType.UTF_8)
 *     .process(stream -&gt; stream
 *         .filter(p -&gt; p.getAge() &gt;= 20)
 *         .collect(Collectors.toList()));
 *
 * // Stream APIで書き込み
 * CsvStreamWriter.builder(Person.class, Paths.get("output.csv"))
 *     .charset(CharsetType.UTF_8_BOM)
 *     .write(persons.stream());
 * </pre>
 *
 * <h2>文字エンコーディング</h2>
 * <p>以下の文字エンコーディングに対応しています：</p>
 * <ul>
 *   <li>UTF-8（BOM有り/無し）</li>
 *   <li>Shift_JIS</li>
 *   <li>EUC-JP</li>
 *   <li>Windows-31J</li>
 * </ul>
 *
 * <h2>ファイル形式</h2>
 * <ul>
 *   <li>CSV - カンマ区切り</li>
 *   <li>TSV - タブ区切り</li>
 * </ul>
 *
 * <h2>マッピング方式</h2>
 * <ul>
 *   <li>ヘッダーベースマッピング（デフォルト） - ヘッダー行の名前でフィールドをマッピング</li>
 *   <li>位置ベースマッピング - カラムの位置でフィールドをマッピング（ヘッダーなしCSV用）</li>
 * </ul>
 *
 * @since 1.0.0
 * @version 2.0.0
 */
package com.example.csv;
