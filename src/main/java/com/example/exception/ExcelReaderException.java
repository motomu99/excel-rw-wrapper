package com.example.exception;

import java.io.IOException;

/**
 * Excel読み込み処理に関する例外の基底クラス
 * IOExceptionを継承し、Excel読み込み特有のエラー情報を提供する
 */
public class ExcelReaderException extends IOException {

    /**
     * 指定されたメッセージで例外を構築
     *
     * @param message エラーメッセージ
     */
    public ExcelReaderException(String message) {
        super(message);
    }

    /**
     * 指定されたメッセージと原因で例外を構築
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public ExcelReaderException(String message, Throwable cause) {
        super(message, cause);
    }
}
