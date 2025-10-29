package com.example.csv;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BomSkipperのテスト
 */
@DisplayName("BomSkipper: BOMスキップ機能のテスト")
class BomSkipperTest {

    @Test
    @DisplayName("UTF-8 BOM付きストリームからBOMをスキップできること")
    void testSkipUtf8Bom() throws IOException {
        // UTF-8 BOM (EF BB BF) + "Hello"
        byte[] bomData = new byte[]{
            (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, // BOM
            'H', 'e', 'l', 'l', 'o'
        };
        
        InputStream inputStream = new ByteArrayInputStream(bomData);
        InputStream skippedStream = BomSkipper.skip(inputStream);
        
        // BOMがスキップされて "Hello" が読めること
        byte[] buffer = new byte[5];
        int bytesRead = skippedStream.read(buffer);
        
        assertEquals(5, bytesRead);
        assertArrayEquals(new byte[]{'H', 'e', 'l', 'l', 'o'}, buffer);
    }

    @Test
    @DisplayName("BOMなしストリームは元のデータを保持すること")
    void testNoBom() throws IOException {
        // BOMなし、いきなり "Hello"
        byte[] data = new byte[]{'H', 'e', 'l', 'l', 'o'};
        
        InputStream inputStream = new ByteArrayInputStream(data);
        InputStream skippedStream = BomSkipper.skip(inputStream);
        
        // データがそのまま読めること
        byte[] buffer = new byte[5];
        int bytesRead = skippedStream.read(buffer);
        
        assertEquals(5, bytesRead);
        assertArrayEquals(new byte[]{'H', 'e', 'l', 'l', 'o'}, buffer);
    }

    @Test
    @DisplayName("3バイト未満のデータでもエラーにならないこと")
    void testLessThan3Bytes() throws IOException {
        // 2バイトのみ
        byte[] data = new byte[]{'H', 'i'};
        
        InputStream inputStream = new ByteArrayInputStream(data);
        InputStream skippedStream = BomSkipper.skip(inputStream);
        
        // データがそのまま読めること
        byte[] buffer = new byte[2];
        int bytesRead = skippedStream.read(buffer);
        
        assertEquals(2, bytesRead);
        assertArrayEquals(new byte[]{'H', 'i'}, buffer);
    }

    @Test
    @DisplayName("1バイトのみのストリームでもエラーにならないこと")
    void testSingleByteStream() throws IOException {
        byte[] data = new byte[]{'A'};
        
        InputStream inputStream = new ByteArrayInputStream(data);
        InputStream skippedStream = BomSkipper.skip(inputStream);
        
        // データがそのまま読めること
        byte[] buffer = new byte[1];
        int bytesRead = skippedStream.read(buffer);
        
        assertEquals(1, bytesRead);
        assertArrayEquals(new byte[]{'A'}, buffer);
    }

    @Test
    @DisplayName("BOMに似た別のバイト列は保持されること")
    void testNonBomSimilarBytes() throws IOException {
        // BOMっぽいけど違うデータ
        byte[] data = new byte[]{
            (byte) 0xEF, (byte) 0xBB, (byte) 0xBA, // BOMじゃない
            'H', 'e', 'l', 'l', 'o'
        };
        
        InputStream inputStream = new ByteArrayInputStream(data);
        InputStream skippedStream = BomSkipper.skip(inputStream);
        
        // 全データがそのまま読めること
        byte[] buffer = new byte[8];
        int bytesRead = skippedStream.read(buffer);
        
        assertEquals(8, bytesRead);
        assertArrayEquals(data, buffer);
    }

    @Test
    @DisplayName("コンストラクタはインスタンス化できないこと")
    void testConstructorThrowsException() throws Exception {
        // リフレクションでコンストラクタ呼び出し
        var constructor = BomSkipper.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        // InvocationTargetExceptionがスローされ、その原因がUnsupportedOperationExceptionであること
        var exception = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
        
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }
}
