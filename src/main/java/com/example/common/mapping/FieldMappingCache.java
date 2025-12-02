package com.example.common.mapping;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import com.opencsv.bean.AbstractBeanField;

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
            Class<?> validatorClass = null;
            Class<? extends AbstractBeanField<?, ?>> converterClass = null;

            // 位置ベースマッピング（カスタムバインド優先）
            CsvCustomBindByPosition customPosition = field.getAnnotation(CsvCustomBindByPosition.class);
            if (customPosition != null) {
                position = customPosition.position();
                @SuppressWarnings("unchecked")
                Class<? extends AbstractBeanField<?, ?>> customConverterClass =
                        (Class<? extends AbstractBeanField<?, ?>>) (Class<?>) customPosition.converter();
                converterClass = customConverterClass;
            } else {
                CsvBindByPosition positionAnnotation = field.getAnnotation(CsvBindByPosition.class);
                if (positionAnnotation != null) {
                    position = positionAnnotation.position();
                }
            }

            // ヘッダーベースマッピング（カスタムバインド優先）
            CsvCustomBindByName customName = field.getAnnotation(CsvCustomBindByName.class);
            if (customName != null) {
                columnName = customName.column();
                // 位置ベース側ですでにconverterClassが設定されていない場合のみ上書き
                if (converterClass == null) {
                    @SuppressWarnings("unchecked")
                    Class<? extends AbstractBeanField<?, ?>> customConverterClass =
                            (Class<? extends AbstractBeanField<?, ?>>) (Class<?>) customName.converter();
                    converterClass = customConverterClass;
                }
            } else {
                CsvBindByName nameAnnotation = field.getAnnotation(CsvBindByName.class);
                if (nameAnnotation != null) {
                    columnName = nameAnnotation.column();
                }
            }

            // Pre-assignment validator アノテーション
            com.opencsv.bean.validators.PreAssignmentValidator validatorAnnotation =
                    field.getAnnotation(com.opencsv.bean.validators.PreAssignmentValidator.class);
            if (validatorAnnotation != null) {
                validatorClass = validatorAnnotation.validator();
            }

            if (columnName != null || position != null) {
                fieldCache.put(field, new FieldMappingInfo(field, columnName, position, validatorClass, converterClass));
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
        
        /** Pre-assignment バリデータクラス（バリデーション用） */
        public final Class<?> validatorClass;

        /** カスタムコンバータークラス（前処理用） */
        public final Class<? extends AbstractBeanField<?, ?>> converterClass;

        /**
         * フィールドマッピング情報を作成
         * 
         * @param field マッピング対象のフィールド
         * @param columnName カラム名
         * @param position カラム位置
         * @param validatorClass Pre-assignment バリデータクラス
         * @param converterClass カスタムコンバータークラス
         */
        public FieldMappingInfo(Field field, String columnName, Integer position,
                               Class<?> validatorClass,
                               Class<? extends AbstractBeanField<?, ?>> converterClass) {
            this.field = field;
            this.field.setAccessible(true);
            this.columnName = columnName;
            this.position = position;
            this.validatorClass = validatorClass;
            this.converterClass = converterClass;
        }
    }
}

