package com.example.common.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.file.Path;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * BOM (Byte Order Mark) の読み書きを統一的に扱うユーティリティクラス
 * 
 * <p>UTF-8のBOM（EF BB BF）の読み込みと書き込みを提供します。
 * {@link BomSkipper} と {@link BomWriter} の機能を統合したクラスです。</p>
 */
@Slf4j
public class BomHandler {
    
    /** UTF-8 BOMのバイト配列 */
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    private BomHandler() {
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
    public static InputStream skipBom(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream, UTF8_BOM.length);
        byte[] bom = new byte[UTF8_BOM.length];
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
     * OutputStreamにBOMを書き込む
     * 
     * <p>UTF-8 BOM (EF BB BF) をストリームに書き込みます。</p>
     * 
     * @param outputStream 書き込み先のOutputStream
     * @throws IOException ストリーム書き込み時のエラー
     */
    public static void writeBom(OutputStream outputStream) throws IOException {
        outputStream.write(UTF8_BOM);
        log.debug("BOMを書き込みました");
    }
    
    /**
     * ファイルにBOMが存在するかどうかを確認する
     * 
     * <p>ファイルの先頭3バイトを読み込んで、UTF-8 BOMの有無を確認します。</p>
     * 
     * @param filePath ファイルパス
     * @return BOMが存在する場合true
     * @throws IOException ファイル読み込みエラー
     */
    public static boolean hasBom(Path filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            byte[] bom = new byte[UTF8_BOM.length];
            int bytesRead = fis.read(bom);
            return isUtf8Bom(bom, bytesRead);
        }
    }
    
    /**
     * UTF-8のBOMかどうかを判定
     * 
     * @param bom 読み込んだバイト配列
     * @param bytesRead 実際に読み込んだバイト数
     * @return UTF-8 BOMの場合true
     */
    private static boolean isUtf8Bom(byte[] bom, int bytesRead) {
        return bytesRead == UTF8_BOM.length 
            && bom[0] == UTF8_BOM[0]
            && bom[1] == UTF8_BOM[1]
            && bom[2] == UTF8_BOM[2];
    }
}

