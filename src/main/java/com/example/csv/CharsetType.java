package com.example.csv;

/**
 * 文字エンコーディングの種類を定義するenum
 *
 * <p>CSVファイルの読み書きに使用する文字エンコーディングを指定します。
 * BOM（Byte Order Mark）の有無も設定できます。</p>
 *
 * <h3>使用例:</h3>
 * <pre>
 * // UTF-8 BOM付きでCSVを読み込む
 * CsvReaderWrapper.builder(Person.class, path)
 *     .charset(CharsetType.UTF_8_BOM)
 *     .read();
 * </pre>
 */
public enum CharsetType {
    /** UTF-8エンコーディング（BOMなし） */
    UTF_8("UTF-8", false),

    /** UTF-8エンコーディング（BOM付き）- Excelなどで開く場合に推奨 */
    UTF_8_BOM("UTF-8", true),

    /** Shift_JISエンコーディング（日本語Windows環境で一般的） */
    S_JIS("Shift_JIS", false),

    /** EUC-JPエンコーディング（Unix/Linux環境で使用） */
    EUC_JP("EUC-JP", false),

    /** Windows-31Jエンコーディング（Shift_JISの拡張版） */
    WINDOWS_31J("Windows-31J", false);

    private final String charsetName;
    private final boolean withBom;

    /**
     * CharsetTypeのコンストラクタ
     *
     * @param charsetName 文字セット名
     * @param withBom BOMを使用するかどうか
     */
    CharsetType(String charsetName, boolean withBom) {
        this.charsetName = charsetName;
        this.withBom = withBom;
    }

    /**
     * 文字セット名を取得
     *
     * @return 文字セット名（例: "UTF-8", "Shift_JIS"）
     */
    public String getCharsetName() {
        return charsetName;
    }

    /**
     * BOM（Byte Order Mark）を使用するかどうかを取得
     *
     * @return BOMを使用する場合true、しない場合false
     */
    public boolean isWithBom() {
        return withBom;
    }
}