package com.example.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * BOM (Byte Order Mark) をスキップするためのユーティリティクラス
 * 
 * <p>UTF-8のBOM（EF BB BF）を検出し、存在する場合はスキップします。
 * BOMが存在しない場合は、ストリームの先頭位置を保持します。</p>
 */
@Slf4j
public class BomSkipper {
    
    private BomSkipper() {
        // ユーティリティクラスのためインスタンス化を禁止
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * InputStreamからBOMをスキップする
     * 
     * <p>UTF-8 BOM (EF BB BF) を検出した場合、BOMをスキップしたストリームを返します。
     * BOMが存在しない場合は、読み込んだバイトを戻して元の位置からのストリームを返します。</p>
     * 
     * @param inputStream 元のInputStream
     * @return BOMをスキップしたInputStream
     * @throws IOException ストリーム読み込み時のエラー
     */
    public static InputStream skip(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, 3);
        byte[] bom = new byte[3];
        int bytesRead = pushbackInputStream.read(bom);
        
        if (isUtf8Bom(bom, bytesRead)) {
            log.debug("BOMを検出してスキップしました");
            // BOMが見つかった場合はスキップ（そのまま返す）
        } else {
            // BOMがない場合は読み込んだバイトを戻す
            pushbackInputStream.unread(bom, 0, bytesRead);
        }
        
        return pushbackInputStream;
    }
    
    /**
     * UTF-8のBOMかどうかを判定
     * 
     * @param bom 読み込んだバイト配列
     * @param bytesRead 実際に読み込んだバイト数
     * @return UTF-8 BOMの場合true
     */
    private static boolean isUtf8Bom(byte[] bom, int bytesRead) {
        return bytesRead == 3 
            && bom[0] == (byte) 0xEF 
            && bom[1] == (byte) 0xBB 
            && bom[2] == (byte) 0xBF;
    }
}

