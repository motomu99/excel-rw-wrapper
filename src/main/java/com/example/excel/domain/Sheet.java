package com.example.excel.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excelシートを表すドメインオブジェクト
 * 
 * <p>シートは複数のテーブル（Table）を含むことができます。
 * このクラスはエンティティとして設計されており、テーブルの追加・削除などの操作を提供します。</p>
 */
public class Sheet {
    
    /** シート名 */
    private final String name;
    
    /** シートに含まれるテーブルのリスト */
    private final List<Table<?>> tables;
    
    /**
     * Sheetを構築
     * 
     * @param name シート名
     * @param tables テーブルのリスト
     */
    private Sheet(String name, List<Table<?>> tables) {
        this.name = name;
        this.tables = new ArrayList<>(tables != null ? tables : List.of());
    }
    
    /**
     * Sheetインスタンスを生成
     * 
     * @param name シート名
     * @return Sheetインスタンス
     */
    public static Sheet of(String name) {
        return new Sheet(name, List.of());
    }
    
    /**
     * Sheetインスタンスを生成（テーブル付き）
     * 
     * @param name シート名
     * @param tables テーブルのリスト
     * @return Sheetインスタンス
     */
    public static Sheet of(String name, List<Table<?>> tables) {
        return new Sheet(name, tables);
    }
    
    /**
     * シート名を取得
     * 
     * @return シート名
     */
    public String getName() {
        return name;
    }
    
    /**
     * テーブルのリストを取得（不変）
     * 
     * @return テーブルのリスト
     */
    public List<Table<?>> getTables() {
        return Collections.unmodifiableList(tables);
    }
    
    /**
     * テーブルを追加
     * 
     * @param <T> Beanの型
     * @param table 追加するテーブル
     * @return 新しいSheetインスタンス（不変性を保証）
     */
    public <T> Sheet addTable(Table<T> table) {
        List<Table<?>> newTables = new ArrayList<>(tables);
        newTables.add(table);
        return new Sheet(name, newTables);
    }
    
    /**
     * テーブルが含まれているか
     * 
     * @return trueの場合、テーブルが含まれている
     */
    public boolean hasTables() {
        return !tables.isEmpty();
    }
    
    /**
     * テーブルの数を取得
     * 
     * @return テーブルの数
     */
    public int getTableCount() {
        return tables.size();
    }
}





