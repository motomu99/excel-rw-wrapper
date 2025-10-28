package com.example.csv;

/**
 * ファイル形式の種類を定義するenum
 *
 * <p>CSVファイルとTSVファイルの形式を定義し、適切な区切り文字を提供します。</p>
 *
 * <h3>使用例:</h3>
 * <pre>
 * // TSV形式でファイルを読み込む
 * CsvReaderWrapper.builder(Person.class, path)
 *     .fileType(FileType.TSV)
 *     .read();
 * </pre>
 */
public enum FileType {
    /** CSV形式（カンマ区切り） - 一般的な表計算データ形式 */
    CSV(","),

    /** TSV形式（タブ区切り） - データにカンマが含まれる場合に有用 */
    TSV("\t");

    private final String delimiter;

    /**
     * FileTypeのコンストラクタ
     *
     * @param delimiter フィールド区切り文字
     */
    FileType(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * フィールド区切り文字を取得
     *
     * @return 区切り文字（"," または "\t"）
     */
    public String getDelimiter() {
        return delimiter;
    }
}
