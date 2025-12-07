package com.example.common.util;

import java.io.IOException;
import java.io.OutputStream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * BOM (Byte Order Mark) を書き込むためのユーティリティクラス
 * 
 * <p>UTF-8のBOM（EF BB BF）をOutputStreamに書き込みます。</p>
 */
@Slf4j
public class BomWriter {
    
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    private BomWriter() {
        // ユーティリティクラスのためインスタンス化を禁止
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * OutputStreamにBOMを書き込む
     * 
     * <p>UTF-8 BOM (EF BB BF) をストリームに書き込みます。</p>
     * 
     * @param outputStream 書き込み先のOutputStream
     * @throws IOException ストリーム書き込み時のエラー
     */
    public static void write(OutputStream outputStream) throws IOException {
        outputStream.write(0xEF);
        outputStream.write(0xBB);
        outputStream.write(0xBF);
        log.debug("BOMを書き込みました");
    }
}

