package com.example.exception;

/**
 * CSV書き込み時に発生するカスタム例外
 * 
 * <p>RuntimeExceptionを継承しており、CSV書き込み処理中のエラーを
 * より明確に表現するために使用します。</p>
 */
public class CsvWriteException extends RuntimeException {
    
    /**
     * メッセージと原因を持つ例外を生成
     * 
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public CsvWriteException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * メッセージのみを持つ例外を生成
     * 
     * @param message エラーメッセージ
     */
    public CsvWriteException(String message) {
        super(message);
    }
}

