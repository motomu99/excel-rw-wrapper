package com.example.csv.exception;

/**
 * ヘッダー行が見つからない場合にスローされる例外
 */
public class HeaderNotFoundException extends ExcelReaderException {

    /**
     * 指定されたメッセージで例外を構築
     *
     * @param message エラーメッセージ
     */
    public HeaderNotFoundException(String message) {
        super(message);
    }

    /**
     * キー列名と探索行数を指定して例外を構築
     *
     * @param keyColumnName キー列名
     * @param searchRows 探索した行数
     */
    public HeaderNotFoundException(String keyColumnName, int searchRows) {
        super(String.format("キー列 '%s' を持つヘッダー行が %d行以内に見つかりませんでした", keyColumnName, searchRows));
    }
}
