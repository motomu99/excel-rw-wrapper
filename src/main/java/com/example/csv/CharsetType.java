package com.example.csv;

/**
 * 文字エンコーディングの種類を定義するenum
 */
public enum CharsetType {
    /** UTF-8 */
    UTF_8("UTF-8"),
    /** Shift_JIS */
    S_JIS("Shift_JIS"),
    /** EUC-JP */
    EUC_JP("EUC-JP"),
    /** Windows-31J */
    WINDOWS_31J("Windows-31J");

    private final String charsetName;

    CharsetType(String charsetName) {
        this.charsetName = charsetName;
    }

    public String getCharsetName() {
        return charsetName;
    }
}