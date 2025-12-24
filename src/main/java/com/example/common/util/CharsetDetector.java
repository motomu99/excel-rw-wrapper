package com.example.common.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.mozilla.universalchardet.UniversalDetector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * 文字コードを自動判別するユーティリティクラス
 * 
 * <p>juniversalchardetライブラリを使用して、ファイルの文字コードを自動判別します。
 * ファイルの先頭部分を読み込んで文字コードを検出します。</p>
 * 
 * <h3>使用例:</h3>
 * <pre>
 * Charset detectedCharset = CharsetDetector.detect(filePath);
 * // 判別できなかった場合はUTF-8を返す
 * </pre>
 */
@Slf4j
public class CharsetDetector {
    
    /** 文字コード判別に使用する最大バイト数 */
    private static final int MAX_BYTES_TO_DETECT = 4096;
    
    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    private CharsetDetector() {
        // ユーティリティクラスのためインスタンス化を禁止
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * ファイルの文字コードを自動判別する
     * 
     * <p>ファイルの先頭部分を読み込んで文字コードを検出します。
     * 判別できない場合はUTF-8をデフォルトとして返します。</p>
     * 
     * @param filePath ファイルパス
     * @return 検出された文字コード（判別できない場合はUTF-8）
     * @throws IOException ファイル読み込みエラー
     */
    public static Charset detect(Path filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            return detect(fis);
        }
    }
    
    /**
     * InputStreamから文字コードを自動判別する
     * 
     * <p>ストリームの先頭部分を読み込んで文字コードを検出します。
     * 判別できない場合はUTF-8をデフォルトとして返します。</p>
     * 
     * @param inputStream 入力ストリーム
     * @return 検出された文字コード（判別できない場合はUTF-8）
     * @throws IOException ストリーム読み込みエラー
     */
    public static Charset detect(InputStream inputStream) throws IOException {
        UniversalDetector detector = new UniversalDetector(null);
        
        byte[] buffer = new byte[4096];
        int bytesRead;
        int totalBytesRead = 0;
        
        // 最大4096バイトまで読み込んで判別を試みる
        while ((bytesRead = inputStream.read(buffer, 0, 
                Math.min(buffer.length, MAX_BYTES_TO_DETECT - totalBytesRead))) > 0 
                && !detector.isDone() 
                && totalBytesRead < MAX_BYTES_TO_DETECT) {
            detector.handleData(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;
        }
        
        detector.dataEnd();
        
        String detectedCharset = detector.getDetectedCharset();
        detector.reset();
        
        if (detectedCharset != null && !detectedCharset.isEmpty()) {
            try {
                Charset charset = Charset.forName(normalizeCharsetName(detectedCharset));
                log.debug("文字コードを自動判別しました: {} -> {}", detectedCharset, charset.name());
                return charset;
            } catch (IllegalArgumentException e) {
                log.warn("検出された文字コード名が無効です: {}, UTF-8を使用します", detectedCharset, e);
                return StandardCharsets.UTF_8;
            }
        } else {
            log.debug("文字コードを判別できませんでした。UTF-8を使用します");
            return StandardCharsets.UTF_8;
        }
    }
    
    /**
     * 文字コード名を正規化する
     * 
     * <p>juniversalchardetが返す文字コード名を、JavaのCharsetで使用できる形式に変換します。</p>
     * 
     * @param charsetName 検出された文字コード名
     * @return 正規化された文字コード名
     */
    private static String normalizeCharsetName(String charsetName) {
        if (charsetName == null) {
            return "UTF-8";
        }
        
        // よくある文字コード名のマッピング
        String normalized = charsetName.trim();
        
        // Shift_JIS系の正規化
        if (normalized.equalsIgnoreCase("SHIFT_JIS") 
                || normalized.equalsIgnoreCase("SJIS")
                || normalized.equalsIgnoreCase("MS932")) {
            return "Shift_JIS";
        }
        
        // UTF-8系の正規化
        if (normalized.equalsIgnoreCase("UTF-8") 
                || normalized.equalsIgnoreCase("UTF8")) {
            return "UTF-8";
        }
        
        // EUC-JP系の正規化
        if (normalized.equalsIgnoreCase("EUC-JP") 
                || normalized.equalsIgnoreCase("EUCJP")) {
            return "EUC-JP";
        }
        
        // Windows-31J系の正規化
        if (normalized.equalsIgnoreCase("WINDOWS-31J") 
                || normalized.equalsIgnoreCase("CP932")) {
            return "Windows-31J";
        }
        
        return normalized;
    }
}

