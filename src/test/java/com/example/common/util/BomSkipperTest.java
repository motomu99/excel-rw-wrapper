package com.example.common.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * BomSkipperのテストクラス
 */
@DisplayName("BomSkipper テスト")
class BomSkipperTest {

    /** UTF-8 BOM */
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Test
    @DisplayName("BOM付きInputStreamからBOMをスキップ")
    void testSkip_WithBom() throws IOException {
        // BOM + "テスト"のバイト配列を作成
        byte[] testData = "テスト".getBytes("UTF-8");
        byte[] dataWithBom = new byte[UTF8_BOM.length + testData.length];
        System.arraycopy(UTF8_BOM, 0, dataWithBom, 0, UTF8_BOM.length);
        System.arraycopy(testData, 0, dataWithBom, UTF8_BOM.length, testData.length);
        
        InputStream inputStream = new ByteArrayInputStream(dataWithBom);
        InputStream result = BomSkipper.skip(inputStream);
        
        // BOMがスキップされ、"テスト"のみ読み込まれる
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(testData, readBytes);
    }

    @Test
    @DisplayName("BOMなしInputStreamはそのまま読み込まれる")
    void testSkip_WithoutBom() throws IOException {
        byte[] testData = "テスト".getBytes("UTF-8");
        InputStream inputStream = new ByteArrayInputStream(testData);
        InputStream result = BomSkipper.skip(inputStream);
        
        // BOMがないので、元のデータがそのまま読み込まれる
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(testData, readBytes);
    }

    @Test
    @DisplayName("BOMの一部のみのデータでも正常処理")
    void testSkip_PartialBom() throws IOException {
        // BOMの最初の2バイトのみ
        byte[] partialBom = new byte[]{(byte) 0xEF, (byte) 0xBB};
        InputStream inputStream = new ByteArrayInputStream(partialBom);
        InputStream result = BomSkipper.skip(inputStream);
        
        // BOMではないので、そのまま読み込まれる
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(partialBom, readBytes);
    }

    @Test
    @DisplayName("BOM類似バイトでもBOM以外はスキップしない")
    void testSkip_SimilarButNotBom() throws IOException {
        // BOMに似ているが異なるバイト列
        byte[] notBom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBC};
        InputStream inputStream = new ByteArrayInputStream(notBom);
        InputStream result = BomSkipper.skip(inputStream);
        
        // BOMではないので、そのまま読み込まれる
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(notBom, readBytes);
    }

    @Test
    @DisplayName("複数回のBOMスキップも正常動作")
    void testSkip_Multiple() throws IOException {
        byte[] testData = "ABC".getBytes("UTF-8");
        byte[] dataWithBom = new byte[UTF8_BOM.length + testData.length];
        System.arraycopy(UTF8_BOM, 0, dataWithBom, 0, UTF8_BOM.length);
        System.arraycopy(testData, 0, dataWithBom, UTF8_BOM.length, testData.length);
        
        // 1回目のスキップ
        InputStream inputStream1 = new ByteArrayInputStream(dataWithBom);
        InputStream result1 = BomSkipper.skip(inputStream1);
        byte[] readBytes1 = result1.readAllBytes();
        
        // 2回目のスキップ（別のストリーム）
        InputStream inputStream2 = new ByteArrayInputStream(dataWithBom);
        InputStream result2 = BomSkipper.skip(inputStream2);
        byte[] readBytes2 = result2.readAllBytes();
        
        // 両方とも同じ結果
        assertArrayEquals(testData, readBytes1);
        assertArrayEquals(testData, readBytes2);
    }

    @Test
    @DisplayName("ユーティリティクラスのインスタンス化は禁止")
    void testConstructorThrowsException() throws Exception {
        java.lang.reflect.Constructor<BomSkipper> constructor = 
            BomSkipper.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        Exception exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance()
        );
        
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    }

    @Test
    @DisplayName("長いデータでもBOMスキップが正常動作")
    void testSkip_LongData() throws IOException {
        // 1000バイトのテストデータ
        byte[] testData = new byte[1000];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i % 256);
        }
        
        byte[] dataWithBom = new byte[UTF8_BOM.length + testData.length];
        System.arraycopy(UTF8_BOM, 0, dataWithBom, 0, UTF8_BOM.length);
        System.arraycopy(testData, 0, dataWithBom, UTF8_BOM.length, testData.length);
        
        InputStream inputStream = new ByteArrayInputStream(dataWithBom);
        InputStream result = BomSkipper.skip(inputStream);
        
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(testData, readBytes);
    }

    @Test
    @DisplayName("ASCII文字列でもBOMなしなら正常処理")
    void testSkip_AsciiText() throws IOException {
        byte[] testData = "Hello World".getBytes("UTF-8");
        InputStream inputStream = new ByteArrayInputStream(testData);
        InputStream result = BomSkipper.skip(inputStream);
        
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(testData, readBytes);
    }

    @Test
    @DisplayName("日本語文字列+BOMで正常処理")
    void testSkip_JapaneseTextWithBom() throws IOException {
        String originalText = "日本語テスト文字列";
        byte[] testData = originalText.getBytes("UTF-8");
        byte[] dataWithBom = new byte[UTF8_BOM.length + testData.length];
        System.arraycopy(UTF8_BOM, 0, dataWithBom, 0, UTF8_BOM.length);
        System.arraycopy(testData, 0, dataWithBom, UTF8_BOM.length, testData.length);
        
        InputStream inputStream = new ByteArrayInputStream(dataWithBom);
        InputStream result = BomSkipper.skip(inputStream);
        
        String resultText = new String(result.readAllBytes(), "UTF-8");
        assertEquals(originalText, resultText);
    }

    @Test
    @DisplayName("1バイトのデータでも正常処理")
    void testSkip_SingleByte() throws IOException {
        byte[] testData = new byte[]{0x41}; // 'A'
        InputStream inputStream = new ByteArrayInputStream(testData);
        InputStream result = BomSkipper.skip(inputStream);
        
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(testData, readBytes);
    }

    @Test
    @DisplayName("2バイトのデータでも正常処理")
    void testSkip_TwoBytes() throws IOException {
        byte[] testData = new byte[]{0x41, 0x42}; // "AB"
        InputStream inputStream = new ByteArrayInputStream(testData);
        InputStream result = BomSkipper.skip(inputStream);
        
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(testData, readBytes);
    }

    @Test
    @DisplayName("BOMのみのストリーム")
    void testSkip_BomOnly() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(UTF8_BOM);
        InputStream result = BomSkipper.skip(inputStream);
        
        // BOMがスキップされるので、空になる
        byte[] readBytes = result.readAllBytes();
        assertEquals(0, readBytes.length);
    }

    @Test
    @DisplayName("連続するBOMのような配列")
    void testSkip_MultipleBomLikeSequences() throws IOException {
        // BOM + BOMのようなバイト列（実際はBOMの後にBOM風のデータ）
        byte[] testData = new byte[UTF8_BOM.length * 2];
        System.arraycopy(UTF8_BOM, 0, testData, 0, UTF8_BOM.length);
        System.arraycopy(UTF8_BOM, 0, testData, UTF8_BOM.length, UTF8_BOM.length);
        
        InputStream inputStream = new ByteArrayInputStream(testData);
        InputStream result = BomSkipper.skip(inputStream);
        
        // 最初のBOMのみスキップされる
        byte[] readBytes = result.readAllBytes();
        assertArrayEquals(UTF8_BOM, readBytes);
    }
}






