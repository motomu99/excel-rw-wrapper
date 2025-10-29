package com.example.csv.exception;

import com.example.csv.CsvReadException;
import com.example.csv.CsvWriteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 各種例外クラスのテスト
 */
@DisplayName("例外クラスのテスト")
class ExceptionTest {

    // ========================================
    // KeyColumnNotFoundException
    // ========================================
    
    @Test
    @DisplayName("KeyColumnNotFoundException: キー列名を指定して例外を生成できること")
    void testKeyColumnNotFoundException() {
        String keyColumnName = "userId";
        KeyColumnNotFoundException exception = new KeyColumnNotFoundException(keyColumnName);
        
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("userId"));
        assertTrue(exception.getMessage().contains("キー列"));
        assertTrue(exception.getMessage().contains("見つかりませんでした"));
        assertInstanceOf(ExcelReaderException.class, exception);
    }

    // ========================================
    // SheetNotFoundException
    // ========================================
    
    @Test
    @DisplayName("SheetNotFoundException: シート名を指定して例外を生成できること")
    void testSheetNotFoundExceptionWithName() {
        String sheetName = "データシート";
        SheetNotFoundException exception = new SheetNotFoundException(sheetName);
        
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("データシート"));
        assertTrue(exception.getMessage().contains("シート"));
        assertTrue(exception.getMessage().contains("見つかりませんでした"));
        assertInstanceOf(ExcelReaderException.class, exception);
    }
    
    @Test
    @DisplayName("SheetNotFoundException: シートインデックスを指定して例外を生成できること")
    void testSheetNotFoundExceptionWithIndex() {
        int sheetIndex = 5;
        SheetNotFoundException exception = new SheetNotFoundException(sheetIndex);
        
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("5"));
        assertTrue(exception.getMessage().contains("シートインデックス"));
        assertTrue(exception.getMessage().contains("見つかりませんでした"));
        assertInstanceOf(ExcelReaderException.class, exception);
    }

    // ========================================
    // UncheckedExcelException
    // ========================================
    
    @Test
    @DisplayName("UncheckedExcelException: メッセージと原因を指定して例外を生成できること")
    void testUncheckedExcelExceptionWithMessage() {
        IOException cause = new IOException("ファイルが読めません");
        String message = "Excel読み込みエラー";
        UncheckedExcelException exception = new UncheckedExcelException(message, cause);
        
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertInstanceOf(RuntimeException.class, exception);
    }
    
    @Test
    @DisplayName("UncheckedExcelException: 原因のみを指定して例外を生成できること")
    void testUncheckedExcelExceptionWithCause() {
        IOException cause = new IOException("ファイルが読めません");
        UncheckedExcelException exception = new UncheckedExcelException(cause);
        
        assertNotNull(exception);
        assertEquals(cause, exception.getCause());
        assertInstanceOf(RuntimeException.class, exception);
    }

    // ========================================
    // CsvReadException
    // ========================================
    
    @Test
    @DisplayName("CsvReadException: メッセージと原因を指定して例外を生成できること")
    void testCsvReadExceptionWithMessageAndCause() {
        IOException cause = new IOException("ファイルが読めません");
        String message = "CSV読み込みエラー";
        CsvReadException exception = new CsvReadException(message, cause);
        
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertInstanceOf(RuntimeException.class, exception);
    }
    
    @Test
    @DisplayName("CsvReadException: メッセージのみを指定して例外を生成できること")
    void testCsvReadExceptionWithMessage() {
        String message = "CSV読み込みエラー";
        CsvReadException exception = new CsvReadException(message);
        
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertInstanceOf(RuntimeException.class, exception);
    }

    // ========================================
    // CsvWriteException
    // ========================================
    
    @Test
    @DisplayName("CsvWriteException: メッセージと原因を指定して例外を生成できること")
    void testCsvWriteExceptionWithMessageAndCause() {
        IOException cause = new IOException("ファイルが書けません");
        String message = "CSV書き込みエラー";
        CsvWriteException exception = new CsvWriteException(message, cause);
        
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
        assertInstanceOf(RuntimeException.class, exception);
    }
    
    @Test
    @DisplayName("CsvWriteException: メッセージのみを指定して例外を生成できること")
    void testCsvWriteExceptionWithMessage() {
        String message = "CSV書き込みエラー";
        CsvWriteException exception = new CsvWriteException(message);
        
        assertNotNull(exception);
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
        assertInstanceOf(RuntimeException.class, exception);
    }
}
