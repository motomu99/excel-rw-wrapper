/**
 * Excel読み込み処理における例外クラスを提供するパッケージ
 *
 * <p>このパッケージには、{@link com.example.csv.ExcelStreamReader}の使用時に
 * 発生する可能性のある例外クラスが含まれています。
 *
 * <h2>例外クラス階層</h2>
 * <ul>
 *   <li>{@link com.example.csv.exception.ExcelReaderException} - 基底例外クラス</li>
 *   <li>{@link com.example.csv.exception.HeaderNotFoundException} - ヘッダー行が見つからない場合</li>
 *   <li>{@link com.example.csv.exception.KeyColumnNotFoundException} - キー列が見つからない場合</li>
 *   <li>{@link com.example.csv.exception.SheetNotFoundException} - シートが見つからない場合</li>
 * </ul>
 *
 * <h2>使用例</h2>
 * <pre>
 * try {
 *     List&lt;Person&gt; persons = ExcelStreamReader.of(Person.class, Paths.get("sample.xlsx"))
 *         .headerKey("名前")
 *         .process(stream -&gt; stream.collect(Collectors.toList()));
 * } catch (HeaderNotFoundException e) {
 *     // ヘッダー行が見つからない場合の処理
 *     System.err.println("ヘッダーエラー: " + e.getMessage());
 * } catch (KeyColumnNotFoundException e) {
 *     // キー列が見つからない場合の処理
 *     System.err.println("キー列エラー: " + e.getMessage());
 * } catch (SheetNotFoundException e) {
 *     // シートが見つからない場合の処理
 *     System.err.println("シートエラー: " + e.getMessage());
 * } catch (ExcelReaderException e) {
 *     // その他のExcel読み込みエラー
 *     System.err.println("Excel読み込みエラー: " + e.getMessage());
 * }
 * </pre>
 *
 * @since 1.0.0
 */
package com.example.csv.exception;
