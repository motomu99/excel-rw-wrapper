package com.example.csv;

/**
 * 改行コードの種類を定義するenum
 */
public enum LineSeparatorType {
    /** CRLF (Windows) */
    CRLF("\r\n"),
    /** LF (Unix/Linux/Mac) */
    LF("\n");

    private final String separator;

    LineSeparatorType(String separator) {
        this.separator = separator;
    }

    public String getSeparator() {
        return separator;
    }
}

