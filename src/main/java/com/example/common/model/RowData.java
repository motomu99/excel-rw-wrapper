package com.example.common.model;

import java.util.Objects;

/**
 * ファイル読み込み時のデータと行番号をまとめて保持するラッパークラス
 *
 * <p>既存のドメインモデルを変更せずに行番号情報を取得したい場合に使用します。</p>
 *
 * <p>このクラスは、データ本体と行番号を分離して管理するため、
 * ドメインモデルの純粋性を保ちながら行番号機能を利用できます。</p>
 *
 * <h3>使用例:</h3>
 * <pre>{@code
 * // 既存のモデルクラス(変更不要)
 * @Data
 * public class Person {
 *     @CsvBindByName(column = "名前")
 *     private String name;
 *
 *     @CsvBindByName(column = "年齢")
 *     private Integer age;
 * }
 *
 * // 使用方法: readWithLineNumber() メソッドを使用
 * List<RowData<Person>> results = CsvReaderWrapper.builder(Person.class, csvFile)
 *     .readWithLineNumber();
 *
 * results.forEach(row -> {
 *     System.out.println("行番号: " + row.getLineNumber());
 *     System.out.println("名前: " + row.getData().getName());
 * });
 *
 * // ストリーム処理での使用例
 * results.stream()
 *     .filter(row -> row.getLineNumber() > 10)
 *     .map(RowData::getData)
 *     .forEach(person -> System.out.println(person.getName()));
 * }</pre>
 *
 * <h3>メリット:</h3>
 * <ul>
 *   <li>ドメインモデルを変更する必要がない</li>
 *   <li>行番号が必要な場合のみ使用できる</li>
 *   <li>既存コードへの影響が最小限</li>
 * </ul>
 *
 * <h3>デメリット:</h3>
 * <ul>
 *   <li>データ取得時に {@code getData()} の呼び出しが必要</li>
 *   <li>ラッピング/アンラッピングの手間がある</li>
 * </ul>
 *
 * @param <T> ラップするデータの型
 * @see LineNumberAware
 * @see ILineNumberAware
 */
public final class RowData<T> {

    /**
     * ファイルの行番号(1始まり)
     */
    private final int lineNumber;

    /**
     * 読み込まれたデータ
     */
    private final T data;

    /**
     * RowDataインスタンスを作成
     *
     * @param lineNumber ファイルの行番号(1始まり)
     * @param data 読み込まれたデータ
     */
    public RowData(int lineNumber, T data) {
        this.lineNumber = lineNumber;
        this.data = data;
    }

    /**
     * 行番号を取得
     *
     * @return ファイルの行番号(1始まり)
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * 読み込まれたデータを取得
     *
     * @return 読み込まれたデータ
     */
    public T getData() {
        return data;
    }

    /**
     * RowDataインスタンスを作成するファクトリメソッド
     *
     * @param <T> データの型
     * @param lineNumber 行番号
     * @param data データ
     * @return RowDataインスタンス
     */
    public static <T> RowData<T> of(int lineNumber, T data) {
        return new RowData<>(lineNumber, data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RowData<?> rowData = (RowData<?>) o;
        return lineNumber == rowData.lineNumber && Objects.equals(data, rowData.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumber, data);
    }

    @Override
    public String toString() {
        return "RowData{"
                + "lineNumber=" + lineNumber
                + ", data=" + data
                + '}';
    }
}
