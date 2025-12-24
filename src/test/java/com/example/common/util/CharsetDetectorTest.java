package com.example.common.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * CharsetDetectorのテストクラス
 */
@DisplayName("CharsetDetector テスト")
class CharsetDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("UTF-8ファイルの文字コードを自動判別")
    void testDetect_Utf8() throws IOException {
        Path testFile = tempDir.resolve("test_utf8.txt");
        Files.write(testFile, "テストデータ".getBytes(StandardCharsets.UTF_8));
        
        Charset detected = CharsetDetector.detect(testFile);
        assertEquals(StandardCharsets.UTF_8, detected);
    }

    @Test
    @DisplayName("Shift_JISファイルの文字コードを自動判別")
    void testDetect_ShiftJis() throws IOException {
        Path testFile = tempDir.resolve("test_sjis.txt");
        Files.write(testFile, "テストデータ".getBytes("Shift_JIS"));
        
        Charset detected = CharsetDetector.detect(testFile);
        assertEquals(Charset.forName("Shift_JIS"), detected);
    }

    @Test
    @DisplayName("EUC-JPファイルの文字コードを自動判別")
    void testDetect_EucJp() throws IOException {
        Path testFile = tempDir.resolve("test_eucjp.txt");
        Files.write(testFile, "テストデータ".getBytes("EUC-JP"));
        
        Charset detected = CharsetDetector.detect(testFile);
        assertEquals(Charset.forName("EUC-JP"), detected);
    }

    @Test
    @DisplayName("Windows-31Jファイルの文字コードを自動判別")
    void testDetect_Windows31J() throws IOException {
        Path testFile = tempDir.resolve("test_windows31j.txt");
        Files.write(testFile, "テストデータ".getBytes("Windows-31J"));
        
        Charset detected = CharsetDetector.detect(testFile);
        // Windows-31JはShift_JISとして検出される可能性がある（互換性があるため）
        assertTrue(detected.equals(Charset.forName("Windows-31J")) 
                || detected.equals(Charset.forName("Shift_JIS")));
    }

    @Test
    @DisplayName("空ファイルの場合はUTF-8を返す")
    void testDetect_EmptyFile() throws IOException {
        Path testFile = tempDir.resolve("test_empty.txt");
        Files.createFile(testFile);
        
        Charset detected = CharsetDetector.detect(testFile);
        assertEquals(StandardCharsets.UTF_8, detected);
    }

    @Test
    @DisplayName("InputStreamから文字コードを自動判別")
    void testDetect_InputStream() throws IOException {
        byte[] data = "テストデータ".getBytes(StandardCharsets.UTF_8);
        InputStream inputStream = new ByteArrayInputStream(data);
        
        Charset detected = CharsetDetector.detect(inputStream);
        assertEquals(StandardCharsets.UTF_8, detected);
    }

    @Test
    @DisplayName("Shift_JISのInputStreamから文字コードを自動判別")
    void testDetect_InputStream_ShiftJis() throws IOException {
        byte[] data = "テストデータ".getBytes("Shift_JIS");
        InputStream inputStream = new ByteArrayInputStream(data);
        
        Charset detected = CharsetDetector.detect(inputStream);
        assertEquals(Charset.forName("Shift_JIS"), detected);
    }

    @Test
    @DisplayName("文字コード名の正規化 - SJIS")
    void testNormalizeCharsetName_Sjis() throws IOException {
        // SJISとして検出されるファイルを作成（十分なデータ量が必要）
        Path testFile = tempDir.resolve("test_sjis.txt");
        String testData = "テストデータ".repeat(10); // データ量を増やす
        Files.write(testFile, testData.getBytes("Shift_JIS"));
        
        Charset detected = CharsetDetector.detect(testFile);
        // Shift_JISとして検出されることを確認
        assertEquals(Charset.forName("Shift_JIS"), detected);
    }

    @Test
    @DisplayName("文字コード名の正規化 - MS932")
    void testNormalizeCharsetName_Ms932() throws IOException {
        // MS932として検出される可能性があるファイル（十分なデータ量が必要）
        Path testFile = tempDir.resolve("test_ms932.txt");
        String testData = "テストデータ".repeat(10); // データ量を増やす
        Files.write(testFile, testData.getBytes("Windows-31J"));
        
        Charset detected = CharsetDetector.detect(testFile);
        // Windows-31JまたはShift_JISとして検出される可能性がある
        assertTrue(detected.equals(Charset.forName("Shift_JIS")) 
                || detected.equals(Charset.forName("Windows-31J")));
    }

    @Test
    @DisplayName("文字コード名の正規化 - UTF8")
    void testNormalizeCharsetName_Utf8() throws IOException {
        Path testFile = tempDir.resolve("test_utf8.txt");
        Files.write(testFile, "テスト".getBytes(StandardCharsets.UTF_8));
        
        Charset detected = CharsetDetector.detect(testFile);
        assertEquals(StandardCharsets.UTF_8, detected);
    }

    @Test
    @DisplayName("文字コード名の正規化 - EUCJP")
    void testNormalizeCharsetName_Eucjp() throws IOException {
        Path testFile = tempDir.resolve("test_eucjp.txt");
        // 十分なデータ量が必要（短いと誤検出される可能性がある）
        String testData = "テストデータ".repeat(20); // データ量を増やす
        Files.write(testFile, testData.getBytes("EUC-JP"));
        
        Charset detected = CharsetDetector.detect(testFile);
        // EUC-JPとして検出されることを確認（データ量が少ないと誤検出される可能性がある）
        assertEquals(Charset.forName("EUC-JP"), detected);
    }

    @Test
    @DisplayName("文字コード名の正規化 - CP932")
    void testNormalizeCharsetName_Cp932() throws IOException {
        Path testFile = tempDir.resolve("test_cp932.txt");
        // 十分なデータ量が必要
        String testData = "テストデータ".repeat(10); // データ量を増やす
        Files.write(testFile, testData.getBytes("Windows-31J"));
        
        Charset detected = CharsetDetector.detect(testFile);
        // Windows-31JまたはShift_JISとして検出される可能性がある
        assertTrue(detected.equals(Charset.forName("Shift_JIS")) 
                || detected.equals(Charset.forName("Windows-31J")));
    }

    @Test
    @DisplayName("ユーティリティクラスのインスタンス化は禁止")
    void testConstructorThrowsException() throws Exception {
        java.lang.reflect.Constructor<CharsetDetector> constructor = 
            CharsetDetector.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        Exception exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance()
        );
        
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    }

    @Test
    @DisplayName("バイナリデータでもUTF-8を返す（フォールバック）")
    void testDetect_BinaryData() throws IOException {
        // バイナリデータ（画像のようなデータ）
        byte[] binaryData = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        Path testFile = tempDir.resolve("test_binary.bin");
        Files.write(testFile, binaryData);
        
        // 判別できない場合はUTF-8を返す
        Charset detected = CharsetDetector.detect(testFile);
        assertEquals(StandardCharsets.UTF_8, detected);
    }

    @Test
    @DisplayName("ASCII文字のみのファイルの文字コード判別")
    void testDetect_AsciiOnly() throws IOException {
        Path testFile = tempDir.resolve("test_ascii.txt");
        Files.write(testFile, "Hello World".getBytes(StandardCharsets.UTF_8));
        
        Charset detected = CharsetDetector.detect(testFile);
        // ASCII文字のみの場合はUS-ASCIIとして検出される可能性がある
        // これは正常な動作（US-ASCIIはUTF-8のサブセット）
        assertTrue(detected.equals(StandardCharsets.UTF_8) 
                || detected.equals(Charset.forName("US-ASCII")));
    }
}

