package com.example.excel.domain;

import java.util.List;

import com.example.common.mapping.FieldMappingCache;

/**
 * Excelシート内の1つのテーブル（ブロック）を表すドメインオブジェクト
 * 
 * <p>テーブルは以下の構造を持ちます：
 * <ul>
 *   <li>ヘッダー行: {@link com.opencsv.bean.CsvBindByName}アノテーションから自動抽出</li>
 *   <li>データ行: Beanオブジェクトのリスト</li>
 * </ul>
 * </p>
 * 
 * <p>このクラスは値オブジェクトとして設計されており、不変性を保証します。</p>
 *
 * @param <T> テーブルに含まれるBeanクラスの型
 */
public class Table<T> {
    
    /** Beanクラス（ヘッダー抽出用） */
    private final Class<T> beanClass;
    
    /** アンカーセル（開始位置、例: "A1", "A25"） */
    private final Anchor anchor;
    
    /** 書き込むデータのリスト */
    private final List<T> data;
    
    /** 位置ベースマッピングを使用するか */
    private final boolean usePositionMapping;
    
    /** フィールドマッピングキャッシュ（ヘッダー抽出用） */
    private final FieldMappingCache fieldMappingCache;
    
    /**
     * Tableを構築
     * 
     * @param beanClass Beanクラス
     * @param anchor アンカーセル
     * @param data 書き込むデータ
     * @param usePositionMapping 位置ベースマッピングを使用するか
     */
    private Table(Class<T> beanClass, Anchor anchor, List<T> data, boolean usePositionMapping) {
        this.beanClass = beanClass;
        this.anchor = anchor;
        this.data = data != null ? List.copyOf(data) : List.of();
        this.usePositionMapping = usePositionMapping;
        this.fieldMappingCache = new FieldMappingCache(beanClass);
    }
    
    /**
     * Beanクラスを取得
     * 
     * @return Beanクラス
     */
    public Class<T> getBeanClass() {
        return beanClass;
    }
    
    /**
     * アンカーセルを取得
     * 
     * @return アンカーセル
     */
    public Anchor getAnchor() {
        return anchor;
    }
    
    /**
     * 書き込むデータを取得
     * 
     * @return データのリスト（不変）
     */
    public List<T> getData() {
        return data;
    }
    
    /**
     * 位置ベースマッピングを使用するか
     * 
     * @return trueの場合、位置ベースマッピングを使用
     */
    public boolean isUsePositionMapping() {
        return usePositionMapping;
    }
    
    /**
     * フィールドマッピングキャッシュを取得
     * 
     * @return フィールドマッピングキャッシュ
     */
    public FieldMappingCache getFieldMappingCache() {
        return fieldMappingCache;
    }
    
    /**
     * テーブルにデータが含まれているか
     * 
     * @return trueの場合、データが含まれている
     */
    public boolean hasData() {
        return !data.isEmpty();
    }
    
    /**
     * テーブルの行数を取得（ヘッダー + データ行数）
     * 
     * @return 行数
     */
    public int getRowCount() {
        return 1 + data.size(); // ヘッダー行 + データ行
    }
    
    /**
     * Builderインスタンスを生成
     * 
     * @param <T> Beanの型
     * @param beanClass 書き込むBeanクラス
     * @return Builderインスタンス
     */
    public static <T> Builder<T> builder(Class<T> beanClass) {
        return new Builder<>(beanClass);
    }
    
    /**
     * TableのBuilderクラス
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    public static class Builder<T> {
        private final Class<T> beanClass;
        // Builderパターンでフィールド名とメソッド名が同じになるのは一般的なパターン
        private Anchor anchor = Anchor.of("A1");
        private List<T> data = List.of();
        private boolean usePositionMapping = false;
        
        private Builder(Class<T> beanClass) {
            this.beanClass = beanClass;
        }
        
        /**
         * アンカーセル（開始位置）を設定
         * 
         * @param anchor アンカーセル文字列（例: "A1", "A25"）
         * @return このBuilderインスタンス
         */
        public Builder<T> anchor(String anchor) {
            this.anchor = Anchor.of(anchor);
            return this;
        }
        
        /**
         * アンカーセル（開始位置）を設定
         * 
         * @param anchor アンカーセルオブジェクト
         * @return このBuilderインスタンス
         */
        public Builder<T> anchor(Anchor anchor) {
            this.anchor = anchor;
            return this;
        }
        
        /**
         * アンカーセル（開始位置）を設定（行・列インデックス指定）
         * 
         * @param row 行インデックス（0始まり、例: 0=A1, 4=B5）
         * @param column 列インデックス（0始まり、例: 0=A列, 1=B列）
         * @return このBuilderインスタンス
         */
        public Builder<T> anchor(int row, int column) {
            this.anchor = Anchor.of(row, column);
            return this;
        }
        
        /**
         * 書き込むデータを設定（List）
         * 
         * @param data 書き込むデータのリスト
         * @return このBuilderインスタンス
         */
        public Builder<T> data(List<T> data) {
            this.data = data != null ? List.copyOf(data) : List.of();
            return this;
        }
        
        /**
         * 位置ベースマッピングを使用
         * 
         * @return このBuilderインスタンス
         */
        public Builder<T> usePositionMapping() {
            this.usePositionMapping = true;
            return this;
        }
        
        /**
         * ヘッダーベースマッピングを使用（デフォルト）
         * 
         * @return このBuilderインスタンス
         */
        public Builder<T> useHeaderMapping() {
            this.usePositionMapping = false;
            return this;
        }
        
        /**
         * Tableを構築
         * 
         * @return Tableインスタンス
         */
        public Table<T> build() {
            return new Table<>(beanClass, anchor, data, usePositionMapping);
        }
    }
}

