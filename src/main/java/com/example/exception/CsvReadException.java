package com.example.exception;

/**
 * CSV読み込み時に発生するカスタム例外
 * 
 * <p>RuntimeExceptionを継承しており、CSV読み込み処理中のエラーを
 * より明確に表現するために使用します。</p>
 */
public class CsvReadException extends RuntimeException {
    
    /**
     * メッセージと原因を持つ例外を生成
     * 
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public CsvReadException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * メッセージのみを持つ例外を生成
     * 
     * @param message エラーメッセージ
     */
    public CsvReadException(String message) {
        super(message);
    }
}

