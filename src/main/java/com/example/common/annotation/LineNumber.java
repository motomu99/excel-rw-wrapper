package com.example.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ファイル読み込み時の行番号を自動的に設定するフィールドに付与するアノテーション
 *
 * <p>このアノテーションが付与されたフィールドには、Excel/CSVファイルから
 * データを読み込む際に、元ファイルの行番号(1始まり)が自動的に設定されます。</p>
 *
 * <h3>使用例1: アノテーションのみ</h3>
 * <pre>{@code
 * @Data
 * public class Person {
 *     @LineNumber
 *     private Integer lineNumber;
 *
 *     @CsvBindByName(column = "名前")
 *     private String name;
 * }
 * }</pre>
 *
 * <h3>使用例2: 抽象クラスを継承(推奨)</h3>
 * <pre>{@code
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * public class Person extends LineNumberAware {
 *     @CsvBindByName(column = "名前")
 *     private String name;
 * }
 * }</pre>
 *
 * <h3>使用例3: インターフェースを実装</h3>
 * <pre>{@code
 * @Data
 * public class Person implements ILineNumberAware {
 *     private Integer lineNumber;
 *
 *     @CsvBindByName(column = "名前")
 *     private String name;
 * }
 * }</pre>
 *
 * <p>注意: フィールドの型は {@code Integer}, {@code Long}, {@code int}, {@code long} のいずれかである必要があります。</p>
 *
 * @see com.example.common.model.LineNumberAware
 * @see com.example.common.model.ILineNumberAware
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LineNumber {
}
