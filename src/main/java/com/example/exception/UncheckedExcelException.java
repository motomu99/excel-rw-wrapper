package com.example.exception;

/**
 * Excel処理中のチェック例外を非チェック例外でラップするためのクラス
 * ストリーミング処理（Stream API）で使用するため、RuntimeExceptionを継承
 */
public class UncheckedExcelException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    public UncheckedExcelException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public UncheckedExcelException(Throwable cause) {
        super(cause);
    }
}

