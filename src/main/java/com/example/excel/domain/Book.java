package com.example.excel.domain;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excelファイル（ブック）を表すドメインオブジェクト
 * 
 * <p>ブックは複数のシート（Sheet）を含むことができます。
 * このクラスはエンティティとして設計されており、シートの追加・削除などの操作を提供します。</p>
 */
public class Book {
    
    /** ファイルパス */
    private final Path filePath;
    
    /** ブックに含まれるシートのリスト */
    private final List<Sheet> sheets;
    
    /** 既存ファイルを読み込むか */
    private final boolean loadExisting;
    
    /**
     * Bookを構築
     * 
     * @param filePath ファイルパス
     * @param sheets シートのリスト
     * @param loadExisting 既存ファイルを読み込むか
     */
    private Book(Path filePath, List<Sheet> sheets, boolean loadExisting) {
        this.filePath = filePath;
        this.sheets = new ArrayList<>(sheets != null ? sheets : List.of());
        this.loadExisting = loadExisting;
    }
    
    /**
     * Bookインスタンスを生成
     * 
     * @param filePath ファイルパス
     * @return Bookインスタンス
     */
    public static Book of(Path filePath) {
        return new Book(filePath, List.of(), false);
    }
    
    /**
     * Bookインスタンスを生成（シート付き）
     * 
     * @param filePath ファイルパス
     * @param sheets シートのリスト
     * @return Bookインスタンス
     */
    public static Book of(Path filePath, List<Sheet> sheets) {
        return new Book(filePath, sheets, false);
    }
    
    /**
     * ファイルパスを取得
     * 
     * @return ファイルパス
     */
    public Path getFilePath() {
        return filePath;
    }
    
    /**
     * シートのリストを取得（不変）
     * 
     * @return シートのリスト
     */
    public List<Sheet> getSheets() {
        return Collections.unmodifiableList(sheets);
    }
    
    /**
     * 既存ファイルを読み込むか
     * 
     * @return trueの場合、既存ファイルを読み込む
     */
    public boolean isLoadExisting() {
        return loadExisting;
    }
    
    /**
     * シートを追加
     * 
     * @param sheet 追加するシート
     * @return 新しいBookインスタンス（不変性を保証）
     */
    public Book addSheet(Sheet sheet) {
        List<Sheet> newSheets = new ArrayList<>(sheets);
        newSheets.add(sheet);
        return new Book(filePath, newSheets, loadExisting);
    }
    
    /**
     * 既存ファイルを読み込む設定を有効化
     * 
     * @return 新しいBookインスタンス（不変性を保証）
     */
    public Book withLoadExisting() {
        return new Book(filePath, sheets, true);
    }
    
    /**
     * シートが含まれているか
     * 
     * @return trueの場合、シートが含まれている
     */
    public boolean hasSheets() {
        return !sheets.isEmpty();
    }
    
    /**
     * シートの数を取得
     * 
     * @return シートの数
     */
    public int getSheetCount() {
        return sheets.size();
    }
}





