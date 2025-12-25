package com.example.common.model;

import com.example.common.annotation.LineNumber;

/**
 * ファイル読み込み時の行番号を自動的に保持する抽象クラス
 *
 * <p>このクラスを継承することで、Excel/CSVファイルからデータを読み込む際に
 * 元ファイルの行番号(1始まり)が自動的に設定されます。</p>
 *
 * <p>継承するだけで行番号フィールドとgetter/setterが自動的に提供されるため、
 * 最も簡単に行番号機能を利用できます。</p>
 *
 * <h3>使用例:</h3>
 * <pre>{@code
 * @Data
 * @EqualsAndHashCode(callSuper = true)
 * public class Person extends LineNumberAware {
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
 *   <li>Javaは単一継承のため、既に他のクラスを継承している場合は使用できません</li>
 *   <li>その場合は {@link ILineNumberAware} インターフェースを実装してください</li>
 *   <li>Lombokの {@code @Data} を使用する場合は {@code @EqualsAndHashCode(callSuper = true)} の指定を推奨</li>
 * </ul>
 *
 * @see LineNumber
 * @see ILineNumberAware
 */
public abstract class LineNumberAware {

    /**
     * ファイルの行番号(1始まり)
     *
     * <p>ヘッダー行がある場合、最初のデータ行は2となります。</p>
     */
    @LineNumber
    private Integer lineNumber;

    /**
     * 行番号を取得
     *
     * @return ファイルの行番号(1始まり)、未設定の場合は {@code null}
     */
    public Integer getLineNumber() {
        return lineNumber;
    }

    /**
     * 行番号を設定
     *
     * <p>このメソッドは通常、ライブラリ内部で自動的に呼び出されます。</p>
     *
     * @param lineNumber ファイルの行番号(1始まり)
     */
    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }
}
