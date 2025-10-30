package com.example.common.mapping;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

/**
 * Beanクラスのフィールドマッピング情報をキャッシュするクラス
 * 
 * <p>リフレクションによるフィールド解析は重いため、一度解析した結果をキャッシュして再利用します。
 * {@link CsvBindByName} および {@link CsvBindByPosition} アノテーションから
 * マッピング情報を抽出します。</p>
 */
public class FieldMappingCache {

    private final Map<Field, FieldMappingInfo> cache;

    /**
     * 指定されたBeanクラスのフィールドマッピングキャッシュを構築
     * 
     * @param beanClass マッピング対象のBeanクラス
     */
    public FieldMappingCache(Class<?> beanClass) {
        this.cache = buildCache(beanClass);
    }

    /**
     * キャッシュされたフィールドマッピング情報を取得
     * 
     * @return フィールドとマッピング情報のMap
     */
    public Map<Field, FieldMappingInfo> getCache() {
        return cache;
    }

    /**
     * フィールドマッピングキャッシュを構築
     * 
     * @param beanClass マッピング対象のBeanクラス
     * @return フィールドとマッピング情報のMap
     */
    private Map<Field, FieldMappingInfo> buildCache(Class<?> beanClass) {
        Map<Field, FieldMappingInfo> fieldCache = new LinkedHashMap<>();
        Field[] fields = beanClass.getDeclaredFields();
        
        for (Field field : fields) {
            String columnName = null;
            Integer position = null;

            // 位置ベースマッピング
            CsvBindByPosition positionAnnotation = field.getAnnotation(CsvBindByPosition.class);
            if (positionAnnotation != null) {
                position = positionAnnotation.position();
            }

            // ヘッダーベースマッピング
            CsvBindByName nameAnnotation = field.getAnnotation(CsvBindByName.class);
            if (nameAnnotation != null) {
                columnName = nameAnnotation.column();
            }

            if (columnName != null || position != null) {
                fieldCache.put(field, new FieldMappingInfo(field, columnName, position));
            }
        }
        
        return fieldCache;
    }

    /**
     * フィールドマッピング情報を保持する内部クラス
     */
    public static class FieldMappingInfo {
        /** マッピング対象のフィールド */
        public final Field field;
        
        /** カラム名（ヘッダーベースマッピング用） */
        public final String columnName;
        
        /** カラム位置（位置ベースマッピング用） */
        public final Integer position;

        /**
         * フィールドマッピング情報を作成
         * 
         * @param field マッピング対象のフィールド
         * @param columnName カラム名
         * @param position カラム位置
         */
        public FieldMappingInfo(Field field, String columnName, Integer position) {
            this.field = field;
            this.field.setAccessible(true);
            this.columnName = columnName;
            this.position = position;
        }
    }
}

