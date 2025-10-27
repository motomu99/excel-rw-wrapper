package com.example.csv;

/**
 * ファイル形式の種類を定義するenum
 */
public enum FileType {
    /** CSV形式 */
    CSV(","),
    /** TSV形式 */
    TSV("\t");

    private final String delimiter;

    FileType(String delimiter) {
        this.delimiter = delimiter;
    }

    public String getDelimiter() {
        return delimiter;
    }
}
