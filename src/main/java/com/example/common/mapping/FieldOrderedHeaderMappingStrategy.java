package com.example.common.mapping;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.opencsv.bean.HeaderColumnNameMappingStrategy;

import lombok.extern.slf4j.Slf4j;

/**
 * フィールド定義順にヘッダーを出力するマッピング戦略
 * 
 * <p>OpenCSVの標準的な{@link HeaderColumnNameMappingStrategy}はヘッダー名をアルファベット順などでソートしてしまいますが、
 * このクラスはBeanクラス内のフィールド定義順序を維持してヘッダーを生成します。</p>
 * 
 * @param <T> Beanの型
 */
@Slf4j
public class FieldOrderedHeaderMappingStrategy<T> extends HeaderColumnNameMappingStrategy<T> {

    @Override
    public void setType(Class<? extends T> type) {
        super.setType(type);
        
        // フィールド定義順に基づいてカラム順序を設定
        FieldMappingCache mappingCache = new FieldMappingCache(type);
        
        // カラム名 -> 定義順インデックス のMapを作成
        Map<String, Integer> orderMap = new HashMap<>();
        AtomicInteger index = new AtomicInteger(0);
        
        mappingCache.getCache().values().stream()
                .filter(info -> info.getColumnName() != null)
                .forEach(info -> orderMap.put(info.getColumnName(), index.getAndIncrement()));

        // コンパレータを作成してセット
        // 書き込み時のカラム順序を制御する
        Comparator<String> definitionOrderComparator = (col1, col2) -> {
            Integer idx1 = orderMap.get(col1);
            Integer idx2 = orderMap.get(col2);
            
            if (idx1 != null && idx2 != null) {
                return idx1.compareTo(idx2);
            }
            // 定義にあるものを優先（通常は全てのカラムが定義にあるはず）
            if (idx1 != null) return -1;
            if (idx2 != null) return 1;
            // 定義にないものはアルファベット順（フォールバック）
            return col1.compareTo(col2);
        };
        
        this.setColumnOrderOnWrite(definitionOrderComparator);
    }
}
