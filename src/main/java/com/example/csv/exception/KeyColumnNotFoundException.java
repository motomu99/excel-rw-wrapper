package com.example.csv.exception;

/**
 * キー列がヘッダー行に見つからない場合にスローされる例外
 */
public class KeyColumnNotFoundException extends ExcelReaderException {

    /**
     * 指定されたメッセージで例外を構築
     *
     * @param message エラーメッセージ
     */
    public KeyColumnNotFoundException(String message) {
        super(message);
    }

    /**
     * キー列名を指定して例外を構築
     *
     * @param keyColumnName キー列名
     */
    public KeyColumnNotFoundException(String keyColumnName) {
        super(String.format("キー列 '%s' がヘッダー行に見つかりませんでした", keyColumnName));
    }
}
