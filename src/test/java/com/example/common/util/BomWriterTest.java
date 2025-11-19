package com.example.common.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BomWriter のユニットテスト")
public class BomWriterTest {

    @Test
    @DisplayName("write(): UTF-8 BOM (EF BB BF) が書き込まれること")
    void testWriteBomBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        BomWriter.write(baos);
        byte[] bytes = baos.toByteArray();

        assertEquals(3, bytes.length, "BOMは3バイトのはず");
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);
    }

    @Test
    @DisplayName("コンストラクタ: ユーティリティクラスはインスタンス化不可であること")
    void testPrivateConstructorThrows() throws Exception {
        Constructor<BomWriter> ctor = BomWriter.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> {
            try {
                ctor.newInstance();
            } catch (Exception e) {
                // 反射経由の例外ラップを剥がす
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (cause instanceof UnsupportedOperationException) {
                    throw (UnsupportedOperationException) cause;
                }
                // 期待外の例外は再スロー
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException(cause);
            }
        });
        assertEquals("Utility class", ex.getMessage());
    }

    @Test
    @DisplayName("write(): OutputStreamのIOExceptionがそのまま伝播すること")
    void testWritePropagatesIOException() {
        OutputStream throwingStream = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("forced");
            }
        };

        IOException ex = assertThrows(IOException.class, () -> BomWriter.write(throwingStream));
        assertEquals("forced", ex.getMessage());
    }
}








