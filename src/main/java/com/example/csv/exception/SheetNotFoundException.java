package com.example.csv.exception;

/**
 * 指定されたシートが見つからない場合にスローされる例外
 */
public class SheetNotFoundException extends ExcelReaderException {

    // メッセージ直接指定のコンストラクタは不要（他とシグネチャ衝突のため削除）

    /**
     * シート名を指定して例外を構築
     *
     * @param sheetName シート名
     */
    public SheetNotFoundException(String sheetName) {
        super(String.format("シート '%s' が見つかりませんでした", sheetName));
    }

    /**
     * シートインデックスを指定して例外を構築
     *
     * @param sheetIndex シートインデックス
     */
    public SheetNotFoundException(int sheetIndex) {
        super(String.format("シートインデックス %d が見つかりませんでした", sheetIndex));
    }
}
