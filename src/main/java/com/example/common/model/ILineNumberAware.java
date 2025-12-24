package com.example.common.model;

import com.example.common.annotation.LineNumber;

/**
 * ファイル読み込み時の行番号を保持する機能を提供するインターフェース
 *
 * <p>既に他のクラスを継承している場合など、{@link LineNumberAware} 抽象クラスを
 * 継承できない場合に使用します。</p>
 *
 * <p>このインターフェースを実装する場合、{@code @LineNumber} アノテーションを付与した
 * {@code lineNumber} フィールドを自分で定義する必要があります。</p>
 *
 * <h3>使用例:</h3>
 * <pre>{@code
 * @Data
 * public class Person extends SomeBaseClass implements ILineNumberAware {
 *     @LineNumber
 *     private Integer lineNumber;
 *
 *     @CsvBindByName(column = "名前")
 *     private String name;
 *
 *     @CsvBindByName(column = "年齢")
 *     private Integer age;
 * }
 *
 * // 使用方法
 * List<Person> results = CsvReaderWrapper.builder()
 *     .file(csvFile)
 *     .clazz(Person.class)
 *     .build()
 *     .read();
 *
 * results.forEach(person -> {
 *     System.out.println("行番号: " + person.getLineNumber());
 *     System.out.println("名前: " + person.getName());
 * });
 * }</pre>
 *
 * <h3>注意事項:</h3>
 * <ul>
 *   <li>実装クラスで {@code @LineNumber} アノテーション付きフィールドの定義が必要です</li>
 *   <li>Lombokの {@code @Data} を使用すれば getter/setter は自動生成されます</li>
 *   <li>より簡単に使用したい場合は {@link LineNumberAware} 抽象クラスの継承を推奨します</li>
 * </ul>
 *
 * @see LineNumber
 * @see LineNumberAware
 */
public interface ILineNumberAware {

    /**
     * 行番号を取得
     *
     * @return ファイルの行番号(1始まり)、未設定の場合は {@code null}
     */
    Integer getLineNumber();

    /**
     * 行番号を設定
     *
     * <p>このメソッドは通常、ライブラリ内部で自動的に呼び出されます。</p>
     *
     * @param lineNumber ファイルの行番号(1始まり)
     */
    void setLineNumber(Integer lineNumber);
}
