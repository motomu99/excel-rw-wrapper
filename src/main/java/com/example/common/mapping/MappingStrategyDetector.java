package com.example.common.mapping;

import java.lang.reflect.Field;
import java.util.Optional;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * Beanクラスのアノテーションからマッピング戦略を自動判定するクラス
 */
@Slf4j
public class MappingStrategyDetector {

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    private MappingStrategyDetector() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Beanクラスのアノテーションに基づいて、適切なマッピング戦略（usePositionMapping）を判定する
     * 
     * @param beanClass 判定対象のBeanクラス
     * @return usePositionMappingの値を含むOptional。
     *         判定できない場合（アノテーションがない場合など）はempty。
     * @throws IllegalArgumentException ヘッダーベースと位置ベースの両方のアノテーションが混在している場合
     */
    public static Optional<Boolean> detectUsePositionMapping(Class<?> beanClass) {
        boolean hasNameMapping = false;
        boolean hasPositionMapping = false;

        // 全フィールドを走査してアノテーションを確認
        // 親クラスのフィールドも含めるため、再帰的にチェックするか、OpenCSVの仕様に合わせてDeclaredFieldsを見る
        // ここではシンプルに定義されたフィールドを見る（FieldMappingCacheと同様のアプローチ）
        // 必要に応じて親クラスも遡るべきだが、まずは直下のフィールドで判定
        
        Class<?> currentClass = beanClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(CsvBindByName.class) || 
                    field.isAnnotationPresent(CsvCustomBindByName.class)) {
                    hasNameMapping = true;
                }
                
                if (field.isAnnotationPresent(CsvBindByPosition.class) || 
                    field.isAnnotationPresent(CsvCustomBindByPosition.class)) {
                    hasPositionMapping = true;
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        if (hasNameMapping && hasPositionMapping) {
            // 両方存在する場合は曖昧なためエラーとする（ユーザーの要望）
            throw new IllegalArgumentException(
                String.format("Beanクラス %s に @CsvBindByName (またはCustom) と @CsvBindByPosition (またはCustom) が混在しています。" +
                              "マッピング戦略を明示的に指定してください (useHeaderMapping() または usePositionMapping())。", 
                              beanClass.getName()));
        }

        if (hasNameMapping) {
            return Optional.of(false); // useHeaderMapping
        }

        if (hasPositionMapping) {
            return Optional.of(true); // usePositionMapping
        }

        return Optional.empty();
    }
}
