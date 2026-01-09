package com.example.excel.reader;

import lombok.Value;

/**
 * Excel読み込み時のエラー行情報を保持するクラス
 * 
 * <p>列数不一致などのエラーが発生した行の情報を保持します。</p>
 */
@Value
public class ExcelReadError {
    
    /** 行番号（1始まり） */
    int lineNumber;
    
    /** 期待される列数 */
    int expectedColumnCount;
    
    /** 実際の列数 */
    int actualColumnCount;
    
    /** エラーメッセージ */
    String message;
    
    /**
     * 列数不一致エラーを作成
     * 
     * @param lineNumber 行番号（1始まり）
     * @param expectedColumnCount 期待される列数
     * @param actualColumnCount 実際の列数
     * @return ExcelReadErrorインスタンス
     */
    public static ExcelReadError columnCountMismatch(int lineNumber, int expectedColumnCount, int actualColumnCount) {
        String message = String.format(
            "列数が不一致です (行番号=%d, 期待値=%d, 実際=%d)",
            lineNumber, expectedColumnCount, actualColumnCount
        );
        return new ExcelReadError(lineNumber, expectedColumnCount, actualColumnCount, message);
    }
    
    /**
     * カスタムエラーを作成
     * 
     * @param lineNumber 行番号（1始まり）
     * @param message エラーメッセージ
     * @return ExcelReadErrorインスタンス
     */
    public static ExcelReadError custom(int lineNumber, String message) {
        return new ExcelReadError(lineNumber, -1, -1, message);
    }
}
