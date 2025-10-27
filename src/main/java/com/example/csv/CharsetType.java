package com.example.csv;

/**
 * 文字エンコーディングの種類を定義するenum
 */
public enum CharsetType {
    /** UTF-8 */
    UTF_8("UTF-8", false),
    /** UTF-8 with BOM */
    UTF_8_BOM("UTF-8", true),
    /** Shift_JIS */
    S_JIS("Shift_JIS", false),
    /** EUC-JP */
    EUC_JP("EUC-JP", false),
    /** Windows-31J */
    WINDOWS_31J("Windows-31J", false);

    private final String charsetName;
    private final boolean withBom;

    CharsetType(String charsetName, boolean withBom) {
        this.charsetName = charsetName;
        this.withBom = withBom;
    }

    public String getCharsetName() {
        return charsetName;
    }

    public boolean isWithBom() {
        return withBom;
    }
}