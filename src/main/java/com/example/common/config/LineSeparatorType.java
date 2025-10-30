package com.example.common.config;

/**
 * 改行コードの種類を定義するenum
 *
 * <p>CSVファイルの書き込み時に使用する改行コードを指定します。
 * プラットフォームに応じて適切な改行コードを選択してください。</p>
 *
 * <h3>使用例:</h3>
 * <pre>
 * // Unix/Linux環境向けにLF改行でCSVを出力
 * CsvWriterWrapper.builder(Person.class, path)
 *     .lineSeparator(LineSeparatorType.LF)
 *     .write(persons);
 * </pre>
 */
public enum LineSeparatorType {
    /** CRLF改行コード（\r\n） - Windows環境で標準 */
    CRLF("\r\n"),

    /** LF改行コード（\n） - Unix/Linux/Mac環境で標準 */
    LF("\n");

    private final String separator;

    /**
     * LineSeparatorTypeのコンストラクタ
     *
     * @param separator 改行コード文字列
     */
    LineSeparatorType(String separator) {
        this.separator = separator;
    }

    /**
     * 改行コード文字列を取得
     *
     * @return 改行コード（"\r\n" または "\n"）
     */
    public String getSeparator() {
        return separator;
    }
}

