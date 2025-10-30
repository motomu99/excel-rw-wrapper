package com.example.common.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * BomHandlerのテストクラス
 */
@DisplayName("BomHandler テスト")
class BomHandlerTest {

    /** UTF-8 BOM */
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Test
    @DisplayName("BOM付きInputStreamからBOMをスキップ")
    void testSkipBom_WithBom() throws IOException {
        // BOM + "テスト"のバイト配列を作成
        byte[] testData = "テスト".getBytes("UTF-8");
        byte[] dataWithBom = new byte[UTF8_BOM.length + testData.length];
        System.arraycopy(UTF8_BOM, 0, dataWithBom, 0, UTF8_BOM.length);
        System.arraycopy(testData, 0, dataWithBom, UTF8_BOM.length, testData.length);
        
        InputStream inputStream = new ByteArrayInputStream(dataWithBom);
        InputStream result = BomHandler.skipBom(inputStream);
        
        // BOMがスキップされ、"テスト"のみ読み込まれる
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(testData, readBytes);
    }

    @Test
    @DisplayName("BOMなしInputStreamはそのまま読み込まれる")
    void testSkipBom_WithoutBom() throws IOException {
        byte[] testData = "テスト".getBytes("UTF-8");
        InputStream inputStream = new ByteArrayInputStream(testData);
        InputStream result = BomHandler.skipBom(inputStream);
        
        // BOMがないので、元のデータがそのまま読み込まれる
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(testData, readBytes);
    }

    @Test
    @DisplayName("BOMの一部のみのデータでも正常処理")
    void testSkipBom_PartialBom() throws IOException {
        // BOMの最初の2バイトのみ
        byte[] partialBom = new byte[]{(byte) 0xEF, (byte) 0xBB};
        InputStream inputStream = new ByteArrayInputStream(partialBom);
        InputStream result = BomHandler.skipBom(inputStream);
        
        // BOMではないので、そのまま読み込まれる
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(partialBom, readBytes);
    }

    @Test
    @DisplayName("BOM類似バイトでもBOM以外はスキップしない")
    void testSkipBom_SimilarButNotBom() throws IOException {
        // BOMに似ているが異なるバイト列
        byte[] notBom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBC};
        InputStream inputStream = new ByteArrayInputStream(notBom);
        InputStream result = BomHandler.skipBom(inputStream);
        
        // BOMではないので、そのまま読み込まれる
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(notBom, readBytes);
    }

    @Test
    @DisplayName("OutputStreamにBOMを書き込む")
    void testWriteBom() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BomHandler.writeBom(outputStream);
        
        byte[] writtenBytes = outputStream.toByteArray();
        assertArrayEquals(UTF8_BOM, writtenBytes);
    }

    @Test
    @DisplayName("OutputStreamにBOMを書き込んでから追加データを書き込む")
    void testWriteBom_WithAdditionalData() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BomHandler.writeBom(outputStream);
        
        byte[] testData = "テスト".getBytes("UTF-8");
        outputStream.write(testData);
        
        byte[] writtenBytes = outputStream.toByteArray();
        
        // BOM + テストデータ
        byte[] expected = new byte[UTF8_BOM.length + testData.length];
        System.arraycopy(UTF8_BOM, 0, expected, 0, UTF8_BOM.length);
        System.arraycopy(testData, 0, expected, UTF8_BOM.length, testData.length);
        
        assertArrayEquals(expected, writtenBytes);
    }

    @Test
    @DisplayName("BOM書き込みとスキップの往復処理")
    void testRoundTrip_BomWriteAndSkip() throws IOException {
        // BOMを書き込み
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BomHandler.writeBom(outputStream);
        
        String testString = "日本語テスト";
        outputStream.write(testString.getBytes("UTF-8"));
        
        // 書き込んだデータを読み込み
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        InputStream result = BomHandler.skipBom(inputStream);
        
        // BOMがスキップされ、元のテキストのみ読み込まれる
        String readString = new String(result.readAllBytes(), "UTF-8");
        assertEquals(testString, readString);
    }

    @Test
    @DisplayName("複数回のBOMスキップも正常動作")
    void testSkipBom_Multiple() throws IOException {
        byte[] testData = "ABC".getBytes("UTF-8");
        byte[] dataWithBom = new byte[UTF8_BOM.length + testData.length];
        System.arraycopy(UTF8_BOM, 0, dataWithBom, 0, UTF8_BOM.length);
        System.arraycopy(testData, 0, dataWithBom, UTF8_BOM.length, testData.length);
        
        // 1回目のスキップ
        InputStream inputStream1 = new ByteArrayInputStream(dataWithBom);
        InputStream result1 = BomHandler.skipBom(inputStream1);
        byte[] readBytes1 = result1.readAllBytes();
        
        // 2回目のスキップ（別のストリーム）
        InputStream inputStream2 = new ByteArrayInputStream(dataWithBom);
        InputStream result2 = BomHandler.skipBom(inputStream2);
        byte[] readBytes2 = result2.readAllBytes();
        
        // 両方とも同じ結果
        assertArrayEquals(testData, readBytes1);
        assertArrayEquals(testData, readBytes2);
    }

    @Test
    @DisplayName("ユーティリティクラスのインスタンス化は禁止")
    void testConstructorThrowsException() throws Exception {
        java.lang.reflect.Constructor<BomHandler> constructor = 
            BomHandler.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        Exception exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance()
        );
        
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    }

    @Test
    @DisplayName("BOM書き込み後のストリームサイズ確認")
    void testWriteBom_StreamSize() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BomHandler.writeBom(outputStream);
        
        // BOMは3バイト
        assertEquals(3, outputStream.size());
    }

    @Test
    @DisplayName("長いデータでもBOMスキップが正常動作")
    void testSkipBom_LongData() throws IOException {
        // 1000バイトのテストデータ
        byte[] testData = new byte[1000];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i % 256);
        }
        
        byte[] dataWithBom = new byte[UTF8_BOM.length + testData.length];
        System.arraycopy(UTF8_BOM, 0, dataWithBom, 0, UTF8_BOM.length);
        System.arraycopy(testData, 0, dataWithBom, UTF8_BOM.length, testData.length);
        
        InputStream inputStream = new ByteArrayInputStream(dataWithBom);
        InputStream result = BomHandler.skipBom(inputStream);
        
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(testData, readBytes);
    }
}

