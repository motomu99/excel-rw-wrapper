package com.example.csv.exception;

/**
 * キー列がヘッダー行に見つからない場合にスローされる例外
 */
public class KeyColumnNotFoundException extends ExcelReaderException {

    // メッセージ直接指定のコンストラクタは不要（他とシグネチャ衝突のため削除）

    /**
     * キー列名を指定して例外を構築
     *
     * @param keyColumnName キー列名
     */
    public KeyColumnNotFoundException(String keyColumnName) {
        super(String.format("キー列 '%s' がヘッダー行に見つかりませんでした", keyColumnName));
    }
}
