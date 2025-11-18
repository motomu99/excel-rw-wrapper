package com.example.excel.domain;

/**
 * Excelセルの位置を表す値オブジェクト
 * 
 * <p>アンカーセル文字列（例: "A1", "B25"）を解析して、行・列インデックスを保持します。
 * このクラスは値オブジェクトとして設計されており、不変性を保証します。</p>
 */
public class Anchor {
    
    /** 行インデックス（0始まり） */
    private final int row;
    
    /** 列インデックス（0始まり） */
    private final int column;
    
    /** 元のアンカーセル文字列 */
    private final String anchorString;
    
    /**
     * Anchorを構築
     * 
     * @param anchorString アンカーセル文字列（例: "A1", "B25"）
     */
    private Anchor(String anchorString) {
        this.anchorString = anchorString;
        int[] pos = parseAnchor(anchorString);
        this.row = pos[0];
        this.column = pos[1];
    }
    
    /**
     * Anchorインスタンスを生成
     * 
     * @param anchorString アンカーセル文字列（例: "A1", "B25"）
     * @return Anchorインスタンス
     */
    public static Anchor of(String anchorString) {
        return new Anchor(anchorString);
    }
    
    /**
     * 行・列インデックスからAnchorを生成
     * 
     * @param row 行インデックス（0始まり）
     * @param column 列インデックス（0始まり）
     * @return Anchorインスタンス
     */
    public static Anchor of(int row, int column) {
        String anchorString = toAnchorString(row, column);
        return new Anchor(anchorString);
    }
    
    /**
     * 行インデックスを取得
     * 
     * @return 行インデックス（0始まり）
     */
    public int getRow() {
        return row;
    }
    
    /**
     * 列インデックスを取得
     * 
     * @return 列インデックス（0始まり）
     */
    public int getColumn() {
        return column;
    }
    
    /**
     * アンカーセル文字列を取得
     * 
     * @return アンカーセル文字列
     */
    public String getAnchorString() {
        return anchorString;
    }
    
    /**
     * アンカーセル文字列（例: "A1"）を解析して行・列インデックスを取得
     * 
     * @param anchor アンカーセル（例: "A1", "B25"）
     * @return [行インデックス, 列インデックス]（0始まり）
     */
    private int[] parseAnchor(String anchor) {
        if (anchor == null || anchor.isEmpty()) {
            return new int[] {0, 0};
        }
        
        // 列名（A-Z, AA-ZZなど）を抽出
        int columnEnd = 0;
        while (columnEnd < anchor.length() && Character.isLetter(anchor.charAt(columnEnd))) {
            columnEnd++;
        }
        
        String columnName = anchor.substring(0, columnEnd).toUpperCase();
        String rowName = anchor.substring(columnEnd);
        
        // 列名を数値に変換（A=0, B=1, ..., Z=25, AA=26, ...）
        int column = 0;
        for (int i = 0; i < columnName.length(); i++) {
            column = column * 26 + (columnName.charAt(i) - 'A' + 1);
        }
        column--; // 0始まりにする
        
        // 行番号を数値に変換（1始まり→0始まり）
        int row = 0;
        if (!rowName.isEmpty()) {
            try {
                row = Integer.parseInt(rowName) - 1; // 1始まり→0始まり
            } catch (NumberFormatException e) {
                // デフォルト値0を使用
            }
        }
        
        return new int[] {row, column};
    }
    
    /**
     * 行・列インデックスからアンカーセル文字列を生成
     * 
     * @param row 行インデックス（0始まり）
     * @param column 列インデックス（0始まり）
     * @return アンカーセル文字列（例: "A1"）
     */
    private static String toAnchorString(int row, int column) {
        // 列名を生成（A-Z, AA-ZZなど）
        StringBuilder columnName = new StringBuilder();
        int col = column + 1; // 1始まりに変換
        while (col > 0) {
            col--;
            columnName.insert(0, (char) ('A' + (col % 26)));
            col /= 26;
        }
        
        // 行番号を生成（1始まり）
        int rowNum = row + 1;
        
        return columnName.toString() + rowNum;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Anchor anchor = (Anchor) obj;
        return row == anchor.row && column == anchor.column;
    }
    
    @Override
    public int hashCode() {
        return java.util.Objects.hash(row, column);
    }
    
    @Override
    public String toString() {
        return anchorString;
    }
}





